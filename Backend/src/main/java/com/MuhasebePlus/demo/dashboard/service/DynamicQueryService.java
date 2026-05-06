package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.template.entity.Template;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DynamicQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<String, Class<?>> ENTITY_MAP = Map.of(
            "INVOICE", Invoice.class,
            "TRANSACTION", Transaction.class,
            "CUSTOMER", Customer.class,
            "PRODUCT", Product.class,
            "STOCK", Stock.class,
            "BANK_ACCOUNT", BankAccount.class,
            "INVOICE_LINE_ITEM", InvoiceLineItem.class,
            "TEMPLATE", Template.class,
            "REPORT", Report.class
    );

    @SuppressWarnings("unchecked")
    public QueryResult executeQuery(Map<String, Object> queryConfig, Long companyId) {
        String dataSource = (String) queryConfig.get("dataSource");
        Class<?> entityClass = ENTITY_MAP.get(dataSource);
        if (entityClass == null) {
            throw new IllegalArgumentException("Unsupported data source: " + dataSource);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<?> root = cq.from(entityClass);

        // Apply base filters (including company_id and isDeleted=false)
        List<Predicate> predicates = buildBasePredicates(cb, root, entityClass, companyId);
        predicates.addAll(buildFilters(cb, root, (List<Map<String, Object>>) queryConfig.getOrDefault("filters", List.of())));

        // Group By
        List<Map<String, Object>> groupByList = (List<Map<String, Object>>) queryConfig.getOrDefault("groupBy", List.of());
        List<Expression<?>> groupExpressions = buildGroupBy(cb, root, groupByList);
        if (!groupExpressions.isEmpty()) {
            cq.groupBy(groupExpressions);
        }

        // Select columns
        Map<String, Object> aggregate = (Map<String, Object>) queryConfig.get("aggregate");
        List<Selection<?>> selections = new ArrayList<>();
        selections.addAll(groupExpressions);

        final String aggregateAlias;
        if (aggregate != null) {
            String func = (String) aggregate.get("function");
            String field = (String) aggregate.get("field");
            aggregateAlias = (String) aggregate.getOrDefault("alias", "value");
            Expression<?> aggExpr = buildAggregate(cb, root, func, field);
            selections.add(aggExpr.alias(aggregateAlias));
        } else {
            aggregateAlias = "value";
            selections.add(root);
        }

        cq.multiselect(selections);
        cq.where(predicates.toArray(new Predicate[0]));

        // HAVING
        List<Map<String, Object>> havingList = (List<Map<String, Object>>) queryConfig.getOrDefault("having", List.of());
        if (!havingList.isEmpty() && aggregate != null) {
            List<Predicate> havingPredicates = buildHavingPredicates(cb, cq, root, havingList, (String) aggregate.get("function"), (String) aggregate.get("field"));
            cq.having(havingPredicates.toArray(new Predicate[0]));
        }

        // Sort
        List<Map<String, Object>> sortList = (List<Map<String, Object>>) queryConfig.getOrDefault("sort", List.of());
        if (!sortList.isEmpty()) {
            List<Order> orders = buildSort(cb, cq, root, sortList, groupExpressions, aggregate, aggregateAlias);
            cq.orderBy(orders);
        }

        // Execute
        var typedQuery = entityManager.createQuery(cq);
        int limit = queryConfig.containsKey("limit") ? ((Number) queryConfig.get("limit")).intValue() : 100;
        typedQuery.setMaxResults(Math.min(limit, 1000));

        List<Tuple> tuples = typedQuery.getResultList();
        List<Map<String, Object>> rows = tuples.stream()
                .map(t -> tupleToMap(t, groupByList, aggregateAlias))
                .collect(Collectors.toList());

        // Aggregate meta
        Map<String, Object> aggregateMeta = Map.of(
                "function", aggregate != null ? aggregate.get("function") : null,
                "field", aggregate != null ? aggregate.get("field") : null
        );

        return new QueryResult(rows, buildColumnMeta(groupByList, aggregate, aggregateAlias), aggregateMeta, (long) rows.size());
    }

    private List<Predicate> buildBasePredicates(CriteriaBuilder cb, Root<?> root, Class<?> entityClass, Long companyId) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("company").get("companyId"), companyId));
        try {
            root.get("isDeleted");
            predicates.add(cb.isFalse(root.get("isDeleted")));
        } catch (IllegalArgumentException e) {
            // no soft delete field
        }
        return predicates;
    }

    private List<Predicate> buildFilters(CriteriaBuilder cb, Root<?> root, List<Map<String, Object>> filters) {
        return filters.stream().map(f -> buildFilterPredicate(cb, root, f)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Predicate buildFilterPredicate(CriteriaBuilder cb, Root<?> root, Map<String, Object> filter) {
        String field = (String) filter.get("field");
        String operator = (String) filter.get("operator");
        Object value = filter.get("value");
        Path<?> path = resolvePath(root, field);

        return switch (operator.toUpperCase()) {
            case "EQ" -> cb.equal(path, coerceValue(path.getJavaType(), value));
            case "NE" -> cb.notEqual(path, coerceValue(path.getJavaType(), value));
            case "GT" -> cb.greaterThan((Path<Comparable>) path, (Comparable) coerceValue(path.getJavaType(), value));
            case "GTE" -> cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) coerceValue(path.getJavaType(), value));
            case "LT" -> cb.lessThan((Path<Comparable>) path, (Comparable) coerceValue(path.getJavaType(), value));
            case "LTE" -> cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) coerceValue(path.getJavaType(), value));
            case "BETWEEN" -> {
                List<?> vals = (List<?>) value;
                yield cb.between((Path<Comparable>) path, (Comparable) coerceValue(path.getJavaType(), vals.get(0)), (Comparable) coerceValue(path.getJavaType(), vals.get(1)));
            }
            case "LIKE" -> cb.like(cb.lower((Path<String>) path), "%" + value.toString().toLowerCase() + "%");
            case "IN" -> path.in(((List<?>) value).stream().map(v -> coerceValue(path.getJavaType(), v)).toList());
            case "IS_NULL" -> cb.isNull(path);
            case "IS_NOT_NULL" -> cb.isNotNull(path);
            default -> null;
        };
    }

    private List<Expression<?>> buildGroupBy(CriteriaBuilder cb, Root<?> root, List<Map<String, Object>> groupByList) {
        return groupByList.stream()
            .<Expression<?>>map(g -> {
                String field = (String) g.get("field");
                String transform = (String) g.get("transform");
                Path<?> path = resolvePath(root, field);
                if ("month".equals(transform) && path.getJavaType() == LocalDate.class) {
                    return cb.function("date_trunc", String.class, cb.literal("month"), path);
                } else if ("year".equals(transform) && path.getJavaType() == LocalDate.class) {
                    return cb.function("date_trunc", String.class, cb.literal("year"), path);
                }
                return path;
            })
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Expression<?> buildAggregate(CriteriaBuilder cb, Root<?> root, String func, String field) {
        Path<?> path = "*".equals(field) || field == null ? root.get(root.getModel().getId(Class.class)) : resolvePath(root, field);
        return switch (func.toUpperCase()) {
            case "COUNT" -> cb.count((Path<Number>) path);
            case "SUM" -> cb.sum((Path<Number>) path);
            case "AVG" -> cb.avg((Path<Number>) path);
            case "MIN" -> cb.min((Path<Number>) path);
            case "MAX" -> cb.max((Path<Number>) path);
            default -> cb.sum((Path<Number>) path);
        };
    }

    private List<Predicate> buildHavingPredicates(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> root, List<Map<String, Object>> havingList, String aggFunc, String aggField) {
        Expression<?> aggExpr = buildAggregate(cb, root, aggFunc, aggField);
        return havingList.stream().map(h -> {
            String operator = (String) h.get("operator");
            Object value = h.get("value");
            return switch (operator.toUpperCase()) {
                case "GT" -> cb.greaterThan((Expression<BigDecimal>) aggExpr, new BigDecimal(value.toString()));
                case "GTE" -> cb.greaterThanOrEqualTo((Expression<BigDecimal>) aggExpr, new BigDecimal(value.toString()));
                case "LT" -> cb.lessThan((Expression<BigDecimal>) aggExpr, new BigDecimal(value.toString()));
                case "LTE" -> cb.lessThanOrEqualTo((Expression<BigDecimal>) aggExpr, new BigDecimal(value.toString()));
                case "EQ" -> cb.equal(aggExpr, coerceValue(aggExpr.getJavaType(), value));
                default -> null;
            };
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Order> buildSort(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> root, List<Map<String, Object>> sortList,
                                   List<Expression<?>> groupExpressions, Map<String, Object> aggregate, String aggregateAlias) {
        return sortList.stream().map(s -> {
            String field = (String) s.get("field");
            String direction = (String) s.getOrDefault("direction", "ASC");
            Expression<?> expr = null;

            if (aggregateAlias.equals(field) && aggregate != null) {
                expr = buildAggregate(cb, root, (String) aggregate.get("function"), (String) aggregate.get("field"));
            } else {
                try {
                    expr = resolvePath(root, field);
                } catch (Exception e) {
                    expr = groupExpressions.stream()
                            .filter(ge -> field.equals(ge.getAlias()))
                            .findFirst()
                            .orElse(null);
                }
            }

            if (expr == null) return null;
            return "DESC".equalsIgnoreCase(direction) ? cb.desc(expr) : cb.asc(expr);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<String, Object> tupleToMap(Tuple tuple, List<Map<String, Object>> groupByList, String aggregateAlias) {
        Map<String, Object> map = new LinkedHashMap<>();
        int idx = 0;
        for (Map<String, Object> gb : groupByList) {
            String field = (String) gb.get("field");
            map.put(field, tuple.get(idx));
            idx++;
        }
        if (tuple.getElements().size() > groupByList.size()) {
            map.put(aggregateAlias, tuple.get(idx));
        }
        return map;
    }

    private List<ColumnMeta> buildColumnMeta(List<Map<String, Object>> groupByList, Map<String, Object> aggregate, String aggregateAlias) {
        List<ColumnMeta> cols = new ArrayList<>();
        for (Map<String, Object> gb : groupByList) {
            String field = (String) gb.get("field");
            cols.add(new ColumnMeta(field, field, "DIMENSION"));
        }
        if (aggregate != null) {
            cols.add(new ColumnMeta(aggregateAlias, (String) aggregate.getOrDefault("alias", aggregateAlias), "MEASURE"));
        }
        return cols;
    }

    private Path<?> resolvePath(Root<?> root, String field) {
        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            Path<?> path = root.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            return path;
        }
        return root.get(field);
    }

    private Object coerceValue(Class<?> targetType, Object value) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        if (targetType == LocalDate.class && value instanceof String s) {
            return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(value.toString());
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value.toString());
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value.toString());
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(value.toString());
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object enumVal = Enum.valueOf((Class<Enum>) targetType, value.toString());
            return enumVal;
        }
        return value;
    }

    public record ColumnMeta(String key, String label, String type) {}
    public record QueryResult(List<Map<String, Object>> rows, List<ColumnMeta> columns,
                               Map<String, Object> aggregateMeta, Long totalCount) {}
}
