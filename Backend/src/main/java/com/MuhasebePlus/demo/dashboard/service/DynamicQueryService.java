package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.dashboard.dto.query.*;
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
import jakarta.persistence.metamodel.SingularAttribute;
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

    private static final Map<String, String> FIELD_LABELS;
    static {
        FIELD_LABELS = new java.util.HashMap<>();
        // INVOICE
        FIELD_LABELS.put("invoiceNumber",         "Fatura No");
        FIELD_LABELS.put("invoiceType",           "Fatura Tipi");
        FIELD_LABELS.put("paymentStatus",         "Ödeme Durumu");
        FIELD_LABELS.put("dueDate",               "Vade Tarihi");
        FIELD_LABELS.put("totalAmount",           "Toplam Tutar");
        FIELD_LABELS.put("subtotal",              "Ara Toplam");
        FIELD_LABELS.put("vatAmount",             "KDV Tutarı");
        FIELD_LABELS.put("customer.name",         "Müşteri Adı");
        FIELD_LABELS.put("customer.city",         "Müşteri Şehir");
        // TRANSACTION
        FIELD_LABELS.put("transactionType",       "İşlem Tipi");
        FIELD_LABELS.put("amount",                "Tutar");
        FIELD_LABELS.put("transactionDate",       "İşlem Tarihi");
        FIELD_LABELS.put("category",              "Kategori");
        FIELD_LABELS.put("account.bankName",      "Banka Adı");
        FIELD_LABELS.put("isRecurring",           "Tekrarlayan");
        // CUSTOMER
        FIELD_LABELS.put("name",                  "Ad");
        FIELD_LABELS.put("email",                 "E-posta");
        FIELD_LABELS.put("taxNumber",             "Vergi No");
        FIELD_LABELS.put("city",                  "Şehir");
        FIELD_LABELS.put("type",                  "Tür");
        FIELD_LABELS.put("phoneNumber",           "Telefon");
        // PRODUCT
        FIELD_LABELS.put("barcode",               "Barkod");
        FIELD_LABELS.put("unit",                  "Birim");
        FIELD_LABELS.put("salePrice",             "Satış Fiyatı");
        FIELD_LABELS.put("costPrice",             "Maliyet Fiyatı");
        FIELD_LABELS.put("vatRate",               "KDV Oranı");
        // STOCK
        FIELD_LABELS.put("product.name",          "Ürün Adı");
        FIELD_LABELS.put("product.barcode",       "Ürün Barkodu");
        FIELD_LABELS.put("quantity",              "Miktar");
        FIELD_LABELS.put("minQuantity",           "Kritik Seviye");
        FIELD_LABELS.put("lastCountDate",         "Son Sayım Tarihi");
        // BANK_ACCOUNT
        FIELD_LABELS.put("bankName",              "Banka Adı");
        FIELD_LABELS.put("iban",                  "IBAN");
        FIELD_LABELS.put("currency",              "Para Birimi");
        // INVOICE_LINE_ITEM
        FIELD_LABELS.put("invoice.invoiceNumber", "Fatura No");
        FIELD_LABELS.put("invoice.invoiceType",   "Fatura Tipi");
        FIELD_LABELS.put("invoice.paymentStatus", "Ödeme Durumu");
        FIELD_LABELS.put("unitPrice",             "Birim Fiyat");
        FIELD_LABELS.put("lineTotal",             "Satır Toplamı");
        // TEMPLATE
        FIELD_LABELS.put("templateCode",          "Şablon Kodu");
        FIELD_LABELS.put("templateName",          "Şablon Adı");
        FIELD_LABELS.put("templateType",          "Şablon Tipi");
        FIELD_LABELS.put("period",                "Dönem");
        // REPORT
        FIELD_LABELS.put("reportType",            "Rapor Tipi");
        FIELD_LABELS.put("startDate",             "Başlangıç Tarihi");
        FIELD_LABELS.put("endDate",               "Bitiş Tarihi");
        FIELD_LABELS.put("format",                "Format");
        FIELD_LABELS.put("fileSize",              "Dosya Boyutu");
        // Common
        FIELD_LABELS.put("createdAt",             "Oluşturma Tarihi");
        FIELD_LABELS.put("value",                 "Değer");
        FIELD_LABELS.put("count",                 "Adet");
    }

    public QueryResult executeQuery(QueryConfigDto config, Long companyId) {
        Class<?> entityClass = ENTITY_MAP.get(config.dataSource());
        if (entityClass == null) {
            throw new IllegalArgumentException("Unsupported data source: " + config.dataSource());
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<?> root = cq.from(entityClass);

        List<Predicate> predicates = buildBasePredicates(cb, root, companyId);
        List<FilterClause> filters = config.filters() != null ? config.filters() : List.of();
        predicates.addAll(buildFilters(cb, root, filters));

        List<GroupByClause> groupByList = config.groupBy() != null ? config.groupBy() : List.of();
        List<Expression<?>> groupExpressions = buildGroupBy(cb, root, groupByList);
        if (!groupExpressions.isEmpty()) {
            cq.groupBy(groupExpressions);
        }

        AggregateClause aggregate = config.aggregate();
        List<Selection<?>> selections = new ArrayList<>(groupExpressions);

        final String aggregateAlias;
        if (aggregate != null) {
            aggregateAlias = aggregate.alias() != null ? aggregate.alias() : "value";
            Expression<?> aggExpr = buildAggregate(cb, root, aggregate.function(), aggregate.field());
            selections.add(aggExpr.alias(aggregateAlias));
        } else {
            aggregateAlias = "value";
            selections.add(root);
        }

        cq.select(cb.tuple(selections.toArray(new Selection<?>[0])));
        cq.where(predicates.toArray(new Predicate[0]));

        List<HavingClause> havingList = config.having() != null ? config.having() : List.of();
        if (!havingList.isEmpty() && aggregate != null) {
            List<Predicate> havingPredicates = buildHavingPredicates(cb, root, havingList, aggregate);
            cq.having(havingPredicates.toArray(new Predicate[0]));
        }

        List<SortClause> sortList = config.sort() != null ? config.sort() : List.of();
        if (!sortList.isEmpty()) {
            List<Order> orders = buildSort(cb, root, sortList, groupExpressions, aggregate, aggregateAlias);
            cq.orderBy(orders);
        }

        var typedQuery = entityManager.createQuery(cq);
        int limit = config.limit() != null ? config.limit() : 100;
        typedQuery.setMaxResults(Math.min(limit, 1000));

        List<Tuple> tuples = typedQuery.getResultList();
        List<Map<String, Object>> rows = tuples.stream()
                .map(t -> tupleToMap(t, groupByList, aggregateAlias))
                .collect(Collectors.toList());

        Map<String, Object> aggregateMeta = Map.of(
                "function", aggregate != null ? aggregate.function() : null,
                "field", aggregate != null ? aggregate.field() : null
        );

        return new QueryResult(rows, buildColumnMeta(groupByList, aggregate, aggregateAlias), aggregateMeta, (long) rows.size());
    }

    private List<Predicate> buildBasePredicates(CriteriaBuilder cb, Root<?> root, Long companyId) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("company").get("companyId"), companyId));
        try {
            root.get("isDeleted");
            predicates.add(cb.isFalse(root.get("isDeleted")));
        } catch (IllegalArgumentException e) {
            // entity has no soft-delete field
        }
        return predicates;
    }

    private List<Predicate> buildFilters(CriteriaBuilder cb, Root<?> root, List<FilterClause> filters) {
        return filters.stream()
                .map(f -> buildFilterPredicate(cb, root, f))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Predicate buildFilterPredicate(CriteriaBuilder cb, Root<?> root, FilterClause filter) {
        Path<?> path = resolvePath(root, filter.field());
        Object value = filter.value();

        return switch (filter.operator()) {
            case EQ       -> cb.equal(path, coerceValue(path.getJavaType(), value));
            case NE       -> cb.notEqual(path, coerceValue(path.getJavaType(), value));
            case GT       -> compareGt(cb, path, coerceValue(path.getJavaType(), value));
            case GTE      -> compareGte(cb, path, coerceValue(path.getJavaType(), value));
            case LT       -> compareLt(cb, path, coerceValue(path.getJavaType(), value));
            case LTE      -> compareLte(cb, path, coerceValue(path.getJavaType(), value));
            case BETWEEN  -> {
                List<?> vals = (List<?>) value;
                yield compareBetween(cb, path,
                        coerceValue(path.getJavaType(), vals.get(0)),
                        coerceValue(path.getJavaType(), vals.get(1)));
            }
            case LIKE     -> cb.like(cb.lower((Path<String>) path), "%" + value.toString().toLowerCase() + "%");
            case IN       -> path.in(((List<?>) value).stream().map(v -> coerceValue(path.getJavaType(), v)).toList());
            case IS_NULL     -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
        };
    }

    private List<Expression<?>> buildGroupBy(CriteriaBuilder cb, Root<?> root, List<GroupByClause> groupByList) {
        return groupByList.stream()
                .<Expression<?>>map(g -> {
                    Path<?> path = resolvePath(root, g.field());
                    if (g.transform() == DateTransform.MONTH && path.getJavaType() == LocalDate.class) {
                        return cb.function("date_trunc", String.class, cb.literal("month"), path);
                    } else if (g.transform() == DateTransform.YEAR && path.getJavaType() == LocalDate.class) {
                        return cb.function("date_trunc", String.class, cb.literal("year"), path);
                    }
                    return path;
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Expression<?> buildAggregate(CriteriaBuilder cb, Root<?> root, AggregateFunction func, String field) {
        Path<?> path = (field == null || "*".equals(field))
                ? root.get(root.getModel().getSingularAttributes().stream()
                        .filter(SingularAttribute::isId)
                        .map(SingularAttribute::getName)
                        .findFirst()
                        .orElse("id"))
                : resolvePath(root, field);
        return switch (func) {
            case COUNT -> cb.count(path);
            case SUM   -> cb.sum((Path<Number>) path);
            case AVG   -> cb.avg((Path<Number>) path);
            case MIN   -> cb.min((Path<Number>) path);
            case MAX   -> cb.max((Path<Number>) path);
        };
    }

    @SuppressWarnings("unchecked")
    private List<Predicate> buildHavingPredicates(CriteriaBuilder cb, Root<?> root,
                                                   List<HavingClause> havingList, AggregateClause aggregate) {
        Expression<? extends Number> aggExpr = (Expression<? extends Number>) buildAggregate(cb, root, aggregate.function(), aggregate.field());
        return havingList.stream().map(h -> {
            Number coerced = (Number) coerceValue(aggExpr.getJavaType(), h.value());
            return switch (h.operator()) {
                case GT  -> cb.gt(aggExpr, coerced);
                case GTE -> cb.ge(aggExpr, coerced);
                case LT  -> cb.lt(aggExpr, coerced);
                case LTE -> cb.le(aggExpr, coerced);
                case EQ  -> cb.equal(aggExpr, coerced);
                default  -> null;
            };
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Order> buildSort(CriteriaBuilder cb, Root<?> root, List<SortClause> sortList,
                                   List<Expression<?>> groupExpressions, AggregateClause aggregate, String aggregateAlias) {
        return sortList.stream().map(s -> {
            String field = s.field();
            boolean desc = s.direction() == SortDirection.DESC;
            Expression<?> expr = null;

            if (aggregateAlias.equals(field) && aggregate != null) {
                expr = buildAggregate(cb, root, aggregate.function(), aggregate.field());
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
            return desc ? cb.desc(expr) : cb.asc(expr);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<String, Object> tupleToMap(Tuple tuple, List<GroupByClause> groupByList, String aggregateAlias) {
        Map<String, Object> map = new LinkedHashMap<>();
        int idx = 0;
        for (GroupByClause gb : groupByList) {
            map.put(gb.field(), tuple.get(idx));
            idx++;
        }
        if (tuple.getElements().size() > groupByList.size()) {
            map.put(aggregateAlias, tuple.get(idx));
        }
        return map;
    }

    private List<ColumnMeta> buildColumnMeta(List<GroupByClause> groupByList, AggregateClause aggregate, String aggregateAlias) {
        List<ColumnMeta> cols = new ArrayList<>();
        for (GroupByClause gb : groupByList) {
            String turkishLabel = FIELD_LABELS.getOrDefault(gb.field(), gb.field());
            cols.add(new ColumnMeta(gb.field(), turkishLabel, "DIMENSION"));
        }
        if (aggregate != null) {
            String aliasKey = aggregate.alias() != null ? aggregate.alias() : aggregateAlias;
            String turkishLabel = FIELD_LABELS.getOrDefault(aliasKey,
                    aggregate.function() != null ? aggregate.function().name() : aliasKey);
            cols.add(new ColumnMeta(aggregateAlias, turkishLabel, "MEASURE"));
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

    // These helpers resolve overload ambiguity by binding Path<?> and value to the same type
    // variable Y within a single method scope. Cast is safe for any field Hibernate maps as Comparable.
    @SuppressWarnings("unchecked")
    private static <Y extends Comparable<? super Y>> Predicate compareGt(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.greaterThan((Path<Y>) path, (Y) value);
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Comparable<? super Y>> Predicate compareGte(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.greaterThanOrEqualTo((Path<Y>) path, (Y) value);
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Comparable<? super Y>> Predicate compareLt(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.lessThan((Path<Y>) path, (Y) value);
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Comparable<? super Y>> Predicate compareLte(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.lessThanOrEqualTo((Path<Y>) path, (Y) value);
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Comparable<? super Y>> Predicate compareBetween(CriteriaBuilder cb, Path<?> path, Object lo, Object hi) {
        return cb.between((Path<Y>) path, (Y) lo, (Y) hi);
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
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumVal = Enum.valueOf((Class<Enum>) targetType, value.toString());
            return enumVal;
        }
        return value;
    }

    public record ColumnMeta(String key, String label, String type) {}
    public record QueryResult(List<Map<String, Object>> rows, List<ColumnMeta> columns,
                               Map<String, Object> aggregateMeta, Long totalCount) {}
}
