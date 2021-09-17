package com.ironhack.gilobank.service.impl;

import com.ironhack.gilobank.dao.*;
import com.ironhack.gilobank.enums.Status;
import com.ironhack.gilobank.repositories.*;
import com.ironhack.gilobank.service.interfaces.ITransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TransactionServiceTest {

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private AccountHolderRepository accountHolderRepository;
    @Autowired
    private LoginDetailsRepository loginDetailsRepository;
    @Autowired
    private SavingsAccountRepository savingsAccountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private Address testAddress1;
    private Address testAddress2;
    private AccountHolder testHolder1;
    private AccountHolder testHolder2;
    private LoginDetails loginDetails1;
    private LoginDetails loginDetails2;
    private SavingsAccount testAccount1;
    private SavingsAccount testAccount2;
    private SavingsAccount testAccount3;

    @BeforeEach
    void setUp() throws ParseException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        LocalDate testDateOfBirth1 = LocalDate.parse("1988-01-01");
        LocalDate testDateOfBirth2 = LocalDate.parse("1994-01-01");

        loginDetails1 = new LoginDetails("hackerman", "ihackthings");
        loginDetails2 = new LoginDetails("testusername2", "testpass2");

        testAddress1 = new Address("1", "Primary Road", "Primary", "PRIMA1");
        testAddress2 = new Address("2", "Mailing Road", "Mailing", "MAILI1");

        testHolder1 = new AccountHolder(loginDetails1, "Test1", "TestSur1", testDateOfBirth1, testAddress1, null);
        testHolder2 = new AccountHolder(loginDetails2, "Test2", "TestSur2", testDateOfBirth2, testAddress2, null);

        testAccount1 = new SavingsAccount(
                "secretKey1",
                testHolder1,                    // Primary Holder
                testHolder2,                    // Secondary Holder
                new BigDecimal("1000"),     // balance
                new BigDecimal("10"),       // penaltyFee
                LocalDate.parse("2011-01-01"),  // open date
                Status.ACTIVE,                  // Status
                new BigDecimal("100"),      // Minimum Balance
                new BigDecimal("1"));      // interestRate
        testAccount2 = new SavingsAccount(
                "secretKey2",
                testHolder1,                    // Primary Holder
                null,
                new BigDecimal("2000"),     // balance
                new BigDecimal("20"),       // penaltyFee
                LocalDate.parse("2012-02-02"),  // open date
                Status.ACTIVE,                  // Status
                new BigDecimal("200"),      // Minimum Balance
                new BigDecimal("2"));      // Interest Rate
        testAccount3 = new SavingsAccount(
                "secretKey3",
                testHolder2,                    // Primary Holder
                null,
                new BigDecimal("3000"),     // balance
                new BigDecimal("30"),       // penaltyFee
                LocalDate.parse("2013-03-03"),  // open date
                Status.ACTIVE,                  // Status
                new BigDecimal("300"),      // Minimum Balance
                new BigDecimal("3"));      // Interest Rate

        loginDetailsRepository.saveAll(List.of(loginDetails1, loginDetails2));
        addressRepository.saveAll(List.of(testAddress1, testAddress2));
        accountHolderRepository.saveAll(List.of(testHolder1, testHolder2));
        savingsAccountRepository.saveAll(List.of(testAccount1, testAccount2, testAccount3));
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        accountHolderRepository.deleteAll();
        loginDetailsRepository.deleteAll();
        addressRepository.deleteAll();
    }

    @Test
    void createTransactionLogCredit_TestValidCreation() {
        var sizeBefore = transactionRepository.findAll().size();
        transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        var sizeAfter = transactionRepository.findAll().size();
        assertEquals(sizeAfter, sizeBefore + 1);
    }

    @Test
    void createTransactionLogCredit_TestValidName() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertTrue(testTransaction.getName().contains("US$ 250.00 credit"));
        assertEquals(testTransaction.getBalanceAfterTransaction(), testAccount1.getBalance());
    }

    @Test
    void createTransactionLogCredit_TestValidBalanceTaken() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertEquals(testTransaction.getBalanceAfterTransaction(), testAccount1.getBalance());
    }

    @Test
    void createTransactionLogCredit_TestDateAutoGenerated() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertTrue(testTransaction.getTimeOfTrns().toString().contains(LocalDate.now().toString()));
    }

    @Test
    void createTransactionLogCredit_TestAmountCorrect() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertEquals(new BigDecimal("250.00"), testTransaction.getAmount());
    }

    // Debits

    @Test
    void createTransactionLogDebit_TestValidCreation() {
        var sizeBefore = transactionRepository.findAll().size();
        Transaction testTransaction = transactionService.createTransactionLogDebit(testAccount1, new BigDecimal("250.00"));
        var sizeAfter = transactionRepository.findAll().size();
        assertEquals(sizeAfter, sizeBefore + 1);
    }

    @Test
    void createTransactionLogDebit_TestValidName() {
        Transaction testTransaction = transactionService.createTransactionLogDebit(testAccount1, new BigDecimal("250.00"));
        assertTrue(testTransaction.getName().contains("US$ 250.00 debit"));
        assertEquals(testTransaction.getBalanceAfterTransaction(), testAccount1.getBalance());
    }

    @Test
    void createTransactionLogDebit_TestValidBalanceTaken() {
        Transaction testTransaction = transactionService.createTransactionLogDebit(testAccount1, new BigDecimal("250.00"));
        assertEquals(testTransaction.getBalanceAfterTransaction(), testAccount1.getBalance());
    }

    @Test
    void createTransactionLogDebit_TestDateAutoGenerated() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertTrue(testTransaction.getTimeOfTrns().toString().contains(LocalDate.now().toString()));
    }

    @Test
    void createTransactionLogDebit_TestAmountCorrect() {
        Transaction testTransaction = transactionService.createTransactionLogCredit(testAccount1, new BigDecimal("250.00"));
        assertEquals(new BigDecimal("250.00"), testTransaction.getAmount());
    }


    @Test
    void findByDateTimeBetween() {
        Transaction testTransaction1 = new Transaction(testAccount1, "Test1", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-01-03T10:15:30"));
        Transaction testTransaction2 = new Transaction(testAccount1, "Test2", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-02-03T10:15:30"));
        Transaction testTransaction3 = new Transaction(testAccount1, "Test2", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-03-03T10:15:30"));
        Transaction testTransaction4 = new Transaction(testAccount1, "Test3", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-04-03T10:15:30"));
        Transaction testTransaction5 = new Transaction(testAccount1, "Test4", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-05-03T10:15:30"));
        Transaction testTransaction6 = new Transaction(testAccount1, "Test5", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-06-03T10:15:30"));
        Transaction testTransaction7 = new Transaction(testAccount1, "Test6", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-07-03T10:15:30"));
        Transaction testTransaction8 = new Transaction(testAccount1, "Test7", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-08-03T10:15:30"));
        Transaction testTransaction9 = new Transaction(testAccount1, "Test8", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-09-03T10:15:30"));

        Transaction testTransaction10 = new Transaction(testAccount2, "Test7", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-01-03T10:15:30"));
        Transaction testTransaction11 = new Transaction(testAccount2, "Test8", new BigDecimal("250.00"), testAccount1.getBalance(), LocalDateTime.parse("2020-02-03T10:15:30"));

        transactionRepository.saveAll(List.of(testTransaction1, testTransaction2, testTransaction3, testTransaction4,
                testTransaction5, testTransaction6, testTransaction7, testTransaction8, testTransaction9));

        var transactionList = transactionService.findByDateTimeBetween(testAccount1, LocalDateTime.parse("2020-01-03T10:15:20"), LocalDateTime.parse("2020-04-03T10:30:30"));
        assertEquals(4, transactionList.size());
    }

}