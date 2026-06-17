package com.MuhasebePlus.demo.seed;

import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.entity.StockMovement;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockMovementRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo verisi tohumlayıcı.
 *
 * <p>{@code app.demo.seed=true} (varsayılan) iken ve veritabanı henüz tohumlanmamışken
 * (yani {@code user@user.net} hesabı yoksa) açılışta tek bir demo şirketi, iki kullanıcı
 * (bir ADMIN + bir USER) ve çok sayıda örnek kayıt (müşteriler, ürünler/stok, faturalar,
 * banka hesapları, finansal hareketler ve çekler) oluşturur.</p>
 *
 * <p>H2 dosya tabanlı olduğundan veriler kalıcıdır; bu yüzden tohumlama idempotenttir ve
 * yalnızca boş bir veritabanında bir kez çalışır. Tüm kayıtlar doğrudan repository üzerinden
 * eklenir (servis katmanı atlanır) — böylece muhasebe entegrasyonu gibi yan etkiler tetiklenmez
 * ve Flyway tohum verisine (PostgreSQL'e özel) ihtiyaç duyulmaz.</p>
 *
 * <p>Giriş bilgileri:</p>
 * <ul>
 *   <li>Yönetici: {@code admin@admin.net} / {@code Sifre1234}</li>
 *   <li>Kullanıcı: {@code user@user.net} / {@code Sifre1234}</li>
 * </ul>
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@admin.net";
    private static final String USER_EMAIL = "user@user.net";
    private static final String DEMO_PASSWORD = "Sifre1234";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final ChequeRepository chequeRepository;
    private final PasswordEncoder passwordEncoder;

    /** Tekrarlanabilir ama çeşitli veri için sabit tohumlu rastgelelik. */
    private final Random rnd = new Random(20260617L);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(USER_EMAIL) || userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Demo tohumlama atlandı: demo hesaplar zaten mevcut.");
            return;
        }

        log.info("Demo verisi tohumlanıyor...");

        Company company = seedCompany();
        seedUsers(company);
        List<Customer> customers = seedCustomers(company);
        List<Product> products = seedProductsAndStock(company);
        List<BankAccount> accounts = seedBankAccounts(company);
        seedInvoices(company, customers, products);
        seedTransactions(company, accounts);
        seedCheques(company, customers);

        log.info("Demo verisi hazır. Giriş -> ADMIN: {} | USER: {} | Şifre: {}",
                ADMIN_EMAIL, USER_EMAIL, DEMO_PASSWORD);
    }

    // ---------------------------------------------------------------------
    // Şirket
    // ---------------------------------------------------------------------
    private Company seedCompany() {
        Company company = Company.builder()
                .companyName("Demo Ticaret A.Ş.")
                .taxOffice("Kadıköy")
                .taxOfficeCode("034250")
                .taxNumber("1234567890")
                .address("Bağdat Caddesi No:128 Kat:4")
                .city("İstanbul")
                .phone("+90 216 555 12 34")
                .email("info@demoticaret.com.tr")
                .tradeRegistryNo("İST-2015/874512")
                .mersisNo("0123456789000015")
                .nace("4651")
                .declarationPeriodType("MONTHLY")
                .corporateTaxType("KURUMLAR_VERGISI")
                .isActive(true)
                .build();
        return companyRepository.save(company);
    }

    // ---------------------------------------------------------------------
    // Kullanıcılar (1 ADMIN + 1 USER)
    // ---------------------------------------------------------------------
    private void seedUsers(Company company) {
        userRepository.save(buildUser(company, "Sistem", "Yöneticisi", ADMIN_EMAIL,
                UserRole.ADMIN, "+90 532 100 00 01", LocalDate.of(1985, 4, 12)));
        userRepository.save(buildUser(company, "Ayşe", "Demir", USER_EMAIL,
                UserRole.USER, "+90 532 100 00 02", LocalDate.of(1992, 9, 3)));
    }

    private User buildUser(Company company, String first, String last, String email,
                           UserRole role, String phone, LocalDate birthDate) {
        User user = new User();
        user.setCompany(company);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setPhoneNumber(phone);
        user.setBirthDate(birthDate);
        user.setActive(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        return user;
    }

    // ---------------------------------------------------------------------
    // Müşteriler / Tedarikçiler
    // ---------------------------------------------------------------------
    private List<Customer> seedCustomers(Company company) {
        Object[][] data = {
                // ad, tür, rol, şehir, açılış bakiyesi, kredi limiti, grup
                {"Yıldız Yapı Malzemeleri Ltd. Şti.", CustomerType.CORPORATE, CustomerRole.BUYER, "İstanbul", 12500.00, 250000.00, "Bayi"},
                {"Anadolu Otomotiv San. Tic. A.Ş.", CustomerType.CORPORATE, CustomerRole.BUYER, "Bursa", 0.00, 500000.00, "Kurumsal"},
                {"Ege Tekstil Üretim Ltd. Şti.", CustomerType.CORPORATE, CustomerRole.BOTH, "İzmir", -8400.00, 150000.00, "Bayi"},
                {"Karadeniz Gıda Dağıtım A.Ş.", CustomerType.CORPORATE, CustomerRole.BUYER, "Trabzon", 33200.00, 300000.00, "Kurumsal"},
                {"Başkent Bilişim Hizmetleri Ltd.", CustomerType.CORPORATE, CustomerRole.BUYER, "Ankara", 4750.00, 120000.00, "Bayi"},
                {"Akdeniz Turizm İşletmeleri A.Ş.", CustomerType.CORPORATE, CustomerRole.BUYER, "Antalya", 0.00, 200000.00, "Kurumsal"},
                {"Mehmet Kaya", CustomerType.INDIVIDUAL, CustomerRole.BUYER, "İstanbul", 1250.00, 25000.00, "Perakende"},
                {"Zeynep Aydın", CustomerType.INDIVIDUAL, CustomerRole.BUYER, "İzmir", 0.00, 15000.00, "Perakende"},
                {"Güneş Elektronik Toptan Tic.", CustomerType.CORPORATE, CustomerRole.SELLER, "İstanbul", -45000.00, 0.00, "Tedarikçi"},
                {"Marmara Kağıt ve Kırtasiye Ltd.", CustomerType.CORPORATE, CustomerRole.SELLER, "Kocaeli", -12300.00, 0.00, "Tedarikçi"},
                {"Toros Lojistik Taşımacılık A.Ş.", CustomerType.CORPORATE, CustomerRole.SELLER, "Mersin", -6800.00, 0.00, "Tedarikçi"},
                {"Ali Vural", CustomerType.INDIVIDUAL, CustomerRole.BUYER, "Eskişehir", 320.00, 10000.00, "Perakende"},
        };

        List<Customer> result = new ArrayList<>();
        int seq = 1;
        for (Object[] row : data) {
            Customer c = new Customer();
            c.setCompany(company);
            c.setName((String) row[0]);
            c.setType((CustomerType) row[1]);
            c.setCustomerRole((CustomerRole) row[2]);
            c.setCity((String) row[3]);
            c.setOpeningBalance(bd((Double) row[4]));
            c.setOpeningBalanceDate(LocalDate.now().minusMonths(8));
            c.setCreditLimit(bd((Double) row[5]));
            c.setCustomerGroup((String) row[6]);
            c.setStatus(CustomerStatus.ACTIVE);
            c.setCurrency(Currency.TRY);
            c.setAddress(row[3] + " / Türkiye");
            String slug = slug((String) row[0]);
            c.setEmail("muhasebe@" + slug + ".com.tr");
            c.setPhoneNumber(randomPhone());
            // Cari hesap kodu: alıcılar 120.xxx, satıcılar/tedarikçiler 320.xxx
            boolean supplier = c.getCustomerRole() == CustomerRole.SELLER;
            c.setAccountCode((supplier ? "320." : "120.") + String.format("%03d", seq));
            if (c.getType() == CustomerType.CORPORATE) {
                c.setTaxNumber(String.format("%010d", 2000000000L + seq * 137L));
                c.setTaxOffice((String) row[3]);
            } else {
                c.setIdentityNumber(String.format("%011d", 10000000000L + seq * 733L));
            }
            c.setIban("TR" + String.format("%024d", 120000000000000000L + seq));
            result.add(c);
            seq++;
        }
        customerRepository.saveAll(result);
        log.info("  {} müşteri eklendi.", result.size());
        return result;
    }

    // ---------------------------------------------------------------------
    // Ürünler + Stok + Açılış stok hareketi
    // ---------------------------------------------------------------------
    private List<Product> seedProductsAndStock(Company company) {
        Object[][] data = {
                // ad, birim, satış fiyatı, maliyet, kdv, stok adedi, min stok
                {"Dizüstü Bilgisayar 15.6\" i7", "Adet", 32500.00, 26800.00, 20.0, 45, 10},
                {"Kablosuz Mouse", "Adet", 449.00, 280.00, 20.0, 320, 50},
                {"Mekanik Klavye TR-Q", "Adet", 1290.00, 850.00, 20.0, 140, 30},
                {"27\" 4K Monitör", "Adet", 8990.00, 7100.00, 20.0, 60, 15},
                {"Lazer Yazıcı Mono", "Adet", 4750.00, 3600.00, 20.0, 38, 8},
                {"A4 Fotokopi Kağıdı 80gr", "Paket", 189.00, 132.00, 20.0, 900, 200},
                {"Toner Kartuş 12A", "Adet", 1450.00, 980.00, 20.0, 110, 25},
                {"USB Bellek 64GB", "Adet", 285.00, 165.00, 20.0, 410, 80},
                {"Harici SSD 1TB", "Adet", 2790.00, 2150.00, 20.0, 75, 20},
                {"HD Webcam 1080p", "Adet", 899.00, 560.00, 20.0, 95, 20},
                {"Bluetooth Kulaklık", "Adet", 1990.00, 1240.00, 20.0, 130, 30},
                {"Ofis Yazılımı Lisansı (Yıllık)", "Adet", 3200.00, 2400.00, 20.0, 200, 0},
                {"Ağ Anahtarı 24 Port", "Adet", 6450.00, 5100.00, 20.0, 28, 5},
                {"Cat6 Ethernet Kablo 5m", "Adet", 149.00, 78.00, 20.0, 540, 100},
                {"Sunucu Rafı 42U", "Adet", 18900.00, 15200.00, 20.0, 12, 3},
                {"Kesintisiz Güç Kaynağı 1500VA", "Adet", 5490.00, 4250.00, 20.0, 40, 10},
        };

        List<Product> products = new ArrayList<>();
        int idx = 1;
        for (Object[] row : data) {
            Product p = Product.builder()
                    .company(company)
                    .barcode(String.format("868%010d", 1000000L + idx))
                    .name((String) row[0])
                    .description((String) row[0] + " — demo ürün kataloğu")
                    .unit((String) row[1])
                    .salePrice(bd((Double) row[2]))
                    .costPrice(bd((Double) row[3]))
                    .vatRate(bd((Double) row[4]))
                    .build();
            products.add(p);
            idx++;
        }
        productRepository.saveAll(products);

        List<Stock> stocks = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        int i = 0;
        for (Object[] row : data) {
            Product p = products.get(i++);
            int qty = (Integer) row[5];
            int minQty = (Integer) row[6];

            stocks.add(Stock.builder()
                    .company(company)
                    .productId(p.getProductId())
                    .quantity(qty)
                    .minQuantity(minQty)
                    .lastCountDate(LocalDateTime.now().minusDays(rnd.nextInt(30) + 1))
                    .build());

            movements.add(StockMovement.builder()
                    .company(company)
                    .productId(p.getProductId())
                    .quantity(qty)
                    .movementType(MovementType.OPENING_BALANCE)
                    .sourceType("OPENING")
                    .unitCost(p.getCostPrice())
                    .reason("Açılış stok devri")
                    .build());
        }
        stockRepository.saveAll(stocks);
        stockMovementRepository.saveAll(movements);
        log.info("  {} ürün + stok + açılış hareketi eklendi.", products.size());
        return products;
    }

    // ---------------------------------------------------------------------
    // Banka / Kasa hesapları
    // ---------------------------------------------------------------------
    private List<BankAccount> seedBankAccounts(Company company) {
        List<BankAccount> accounts = new ArrayList<>();
        accounts.add(buildAccount(company, "Ziraat Bankası - Vadesiz TL", BankAccountType.BANK, Currency.TRY, "102.001",
                "TR330006200000100000000001"));
        accounts.add(buildAccount(company, "İş Bankası - Ticari TL", BankAccountType.BANK, Currency.TRY, "102.002",
                "TR640006400000100000000002"));
        accounts.add(buildAccount(company, "Garanti BBVA - USD Hesap", BankAccountType.BANK, Currency.USD, "102.003",
                "TR980006200000100000000003"));
        accounts.add(buildAccount(company, "Merkez Kasa - TL", BankAccountType.CASH, Currency.TRY, "100.001", null));
        bankAccountRepository.saveAll(accounts);
        log.info("  {} banka/kasa hesabı eklendi.", accounts.size());
        return accounts;
    }

    private BankAccount buildAccount(Company company, String name, BankAccountType type,
                                     Currency currency, String code, String iban) {
        BankAccount a = new BankAccount();
        a.setCompany(company);
        a.setBankName(name);
        a.setAccountType(type);
        a.setCurrency(currency);
        a.setAccountCode(code);
        a.setIban(iban);
        return a;
    }

    // ---------------------------------------------------------------------
    // Faturalar + kalemleri (satış ağırlıklı, çeşitli ödeme durumları)
    // ---------------------------------------------------------------------
    private void seedInvoices(Company company, List<Customer> customers, List<Product> products) {
        int invoiceCount = 36;
        long saleSeq = 1;
        long purchaseSeq = 1;
        int year = LocalDate.now().getYear();

        List<Invoice> invoices = new ArrayList<>();
        List<InvoiceLineItem> allLines = new ArrayList<>();

        for (int n = 0; n < invoiceCount; n++) {
            boolean sale = rnd.nextInt(100) < 75; // ~%75 satış
            InvoiceType type = sale ? InvoiceType.sale : InvoiceType.purchase;

            // Satışta alıcı müşteri, alışta tedarikçi seç
            Customer customer = pickCustomer(customers, sale);
            LocalDate invoiceDate = LocalDate.now().minusDays(rnd.nextInt(175) + 2);
            LocalDate dueDate = invoiceDate.plusDays(30);

            // Kalemleri kur
            int lineCount = 1 + rnd.nextInt(4);
            List<InvoiceLineItem> lines = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal vatTotal = BigDecimal.ZERO;
            for (int l = 0; l < lineCount; l++) {
                Product p = products.get(rnd.nextInt(products.size()));
                int qty = 1 + rnd.nextInt(10);
                BigDecimal unitPrice = sale ? p.getSalePrice() : p.getCostPrice();
                BigDecimal net = unitPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal vat = net.multiply(p.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                InvoiceLineItem line = InvoiceLineItem.builder()
                        .company(company)
                        .productId(p.getProductId())
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .vatRate(p.getVatRate())
                        .vatBaseAmount(net)
                        .vatAmount(vat)
                        .lineTotal(net)
                        .discountRate(BigDecimal.ZERO)
                        .build();
                lines.add(line);
                subtotal = subtotal.add(net);
                vatTotal = vatTotal.add(vat);
            }
            BigDecimal total = subtotal.add(vatTotal);

            String number = sale
                    ? String.format("DMO%d%06d", year, saleSeq++)
                    : String.format("ALI%d%06d", year, purchaseSeq++);

            Invoice invoice = Invoice.builder()
                    .company(company)
                    .invoiceNumber(number)
                    .customerId(customer.getCustomerId())
                    .invoiceType(type)
                    .invoiceDate(invoiceDate)
                    .dueDate(dueDate)
                    .paymentStatus(pickStatus(dueDate))
                    .subtotal(subtotal)
                    .vatAmount(vatTotal)
                    .totalAmount(total)
                    .currency(Currency.TRY)
                    .exchangeRate(BigDecimal.ONE)
                    .withholdingTaxAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .description(sale ? "Mal satışı" : "Mal alışı")
                    .cancelled(false)
                    .build();

            Invoice saved = invoiceRepository.save(invoice);
            for (InvoiceLineItem line : lines) {
                line.setInvoiceId(saved.getInvoiceId());
            }
            allLines.addAll(lines);
            invoices.add(saved);
        }
        invoiceLineItemRepository.saveAll(allLines);
        log.info("  {} fatura ({} kalem) eklendi.", invoices.size(), allLines.size());
    }

    private Customer pickCustomer(List<Customer> customers, boolean sale) {
        // Satış -> BUYER/BOTH; Alış -> SELLER/BOTH
        List<Customer> pool = new ArrayList<>();
        for (Customer c : customers) {
            CustomerRole r = c.getCustomerRole();
            if (sale && (r == CustomerRole.BUYER || r == CustomerRole.BOTH)) {
                pool.add(c);
            } else if (!sale && (r == CustomerRole.SELLER || r == CustomerRole.BOTH)) {
                pool.add(c);
            }
        }
        if (pool.isEmpty()) {
            pool = customers;
        }
        return pool.get(rnd.nextInt(pool.size()));
    }

    private PaymentStatus pickStatus(LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) {
            // Vadesi geçmiş: çoğunlukla ödenmiş, bir kısmı gecikmiş
            int r = rnd.nextInt(100);
            if (r < 60) return PaymentStatus.paid;
            if (r < 85) return PaymentStatus.overdue;
            return PaymentStatus.partially_paid;
        }
        int r = rnd.nextInt(100);
        if (r < 45) return PaymentStatus.pending;
        if (r < 70) return PaymentStatus.paid;
        if (r < 85) return PaymentStatus.partially_paid;
        return PaymentStatus.draft;
    }

    // ---------------------------------------------------------------------
    // Finansal hareketler
    // ---------------------------------------------------------------------
    private void seedTransactions(Company company, List<BankAccount> accounts) {
        String[] incomeDesc = {"Müşteri tahsilatı", "POS satış geliri", "Faiz geliri", "Peşin satış", "Avans tahsilatı"};
        String[] expenseDesc = {"Personel maaş ödemesi", "Ofis kira ödemesi", "Elektrik faturası", "Tedarikçi ödemesi",
                "Kargo / lojistik", "Vergi ödemesi", "İnternet & telefon", "Bakım-onarım"};

        List<Transaction> txns = new ArrayList<>();
        int count = 48;
        for (int i = 0; i < count; i++) {
            BankAccount acc = accounts.get(rnd.nextInt(accounts.size()));
            boolean income = rnd.nextInt(100) < 55;
            String desc = income
                    ? incomeDesc[rnd.nextInt(incomeDesc.length)]
                    : expenseDesc[rnd.nextInt(expenseDesc.length)];

            double base = income ? (2000 + rnd.nextInt(48000)) : (1000 + rnd.nextInt(30000));

            Transaction t = new Transaction();
            t.setCompany(company);
            t.setAccountId(acc.getAccountId());
            t.setTransactionType(income ? TransactionType.INCOME : TransactionType.EXPENSE);
            t.setTransactionCategory(categoryFor(acc, income));
            t.setAmount(bd(base));
            t.setTransactionDate(LocalDate.now().minusDays(rnd.nextInt(180)));
            t.setDescription(desc);
            t.setCategory(desc);
            t.setRecurring(desc.contains("maaş") || desc.contains("kira"));
            txns.add(t);
        }
        transactionRepository.saveAll(txns);
        log.info("  {} finansal hareket eklendi.", txns.size());
    }

    private TransactionCategory categoryFor(BankAccount acc, boolean income) {
        if (acc.getAccountType() == BankAccountType.POS && income) {
            return TransactionCategory.POS_COLLECTION;
        }
        return TransactionCategory.GENERAL;
    }

    // ---------------------------------------------------------------------
    // Çek & Senetler
    // ---------------------------------------------------------------------
    private void seedCheques(Company company, List<Customer> customers) {
        String[] banks = {"Ziraat Bankası", "İş Bankası", "Garanti BBVA", "Yapı Kredi", "Akbank", "QNB"};
        ChequeStatus[] statuses = {ChequeStatus.IN_PORTFOLIO, ChequeStatus.DEPOSITED,
                ChequeStatus.COLLECTED, ChequeStatus.IN_PORTFOLIO, ChequeStatus.BOUNCED};

        List<Cheque> cheques = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            boolean receivable = rnd.nextInt(100) < 65;
            Customer customer = customers.get(rnd.nextInt(customers.size()));
            LocalDate issue = LocalDate.now().minusDays(rnd.nextInt(90) + 10);
            LocalDate due = issue.plusDays(30 + rnd.nextInt(120));

            Cheque cheque = Cheque.builder()
                    .company(company)
                    .chequeNumber(String.format("%010d", 7000000000L + i))
                    .chequeType(receivable ? ChequeType.RECEIVABLE : ChequeType.PAYABLE)
                    .chequeKind(rnd.nextInt(100) < 80 ? ChequeKind.CHEQUE : ChequeKind.PROMISSORY_NOTE)
                    .customerId(customer.getCustomerId())
                    .drawerBank(banks[rnd.nextInt(banks.length)])
                    .drawerBranch("Merkez Şube")
                    .drawerAccountNumber(String.format("%08d", rnd.nextInt(99999999)))
                    .amount(bd(5000 + rnd.nextInt(95000)))
                    .currency(Currency.TRY)
                    .issueDate(issue)
                    .dueDate(due)
                    .status(statuses[rnd.nextInt(statuses.length)])
                    .notes(receivable ? "Müşteriden alınan çek" : "Tedarikçiye verilen çek")
                    .build();
            cheques.add(cheque);
        }
        chequeRepository.saveAll(cheques);
        log.info("  {} çek/senet eklendi.", cheques.size());
    }

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------
    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String randomPhone() {
        return String.format("+90 5%02d %03d %02d %02d",
                rnd.nextInt(60), rnd.nextInt(1000), rnd.nextInt(100), rnd.nextInt(100));
    }

    private String slug(String name) {
        String s = name.toLowerCase()
                .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c")
                .replaceAll("[^a-z0-9]+", "");
        return s.length() > 18 ? s.substring(0, 18) : s;
    }
}
