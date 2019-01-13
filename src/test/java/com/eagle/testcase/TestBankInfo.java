package com.eagle.testcase;


import eagle.dao.HttpClientResult;
import eagle.util.HttpRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-context.xml")
public class TestBankInfo {
    @Autowired
    private HttpRequest httpRequest;
    @Value("${app.endpoint}")
    private String url;
    private HttpClientResult result;

    //Init http util
    @PostConstruct
    private void init() {
        httpRequest
                .setHttpHeader("{\"content-type\": \"application/json;charset=UTF-8\"}");
        httpRequest.setUrl(url);
    }

    //Trance log
    @Rule
    public TestName testName = new TestName();
    Log logger = LogFactory.getLog(this.getClass());

    @Before
    public void testStart() {
        logger.info("Start:*********" + testName.getMethodName() + "*********");
    }

    @After
    public void testEnd() {
        logger.info("End:***********" + testName.getMethodName() + "*********");
    }

    //Test Date
    private String[] method = {"LOCAL", "SWIFT"};
    private String[][] country_code_invalid = {{"US", "12"}, {"AU", "1234567"}, {"CN", "12345678"}};
    private String[] account_name_valid = {"ea", "1234567890", "我!@#$%^&*("};
    private String[] account_name_invalid = {"你", "12345678901"};
    private String[][] us_account_number_valid = {{"US", "$"}, {"US", "!@#$%^&*()_+<>?"}, {"US", "12345678901234567"}};
    private String[][] us_account_number_invalid = {{"US", "123456789012345678"}};
    private String[][] au_account_number_valid = {{"AU", "123456"}, {"AU", "!@#$%^&"}, {"AU", "123456789"}};
    private String[][] au_account_number_invalid = {{"AU", "12345"}, {"AU", "1234567890"}};
    private String[][] cn_account_number_valid = {{"CN", "12345678"}, {"CN", "1234UJGRH0"}, {"CN", "12345678901234567890"}};
    private String[][] cn_account_number_invalid = {{"CN", "123456789012345678901"}, {"CN", "1234567"}};
    private String[][] swift_match_country_valid = {{"US","1","1234US23"},{"AU","123456","1234AU23"},{"CN","12345678","1234CN23}"}};
    private String[][] swift_match_country_invalid = {{"US","1","1234AU23"},{"AU","123456","1234CN23"},{"CN","12345678","12345CN23}"}};
    private String[][] swift_length_valid = {{"US","1","1234US78"},{"CN","12345678","1234CN7890A}"}};
    private String[][] swift_length_invalid = {{"US","1","1234US7"},{"CN","12345678","1234CN7890AB}"},{"AU","123456","1234AU789"}};
    private String[][] bsb_valid={{"AU","12345678","123456"},{"CN","12345678","12345678"},{"US","1",""}};
    private String[][] bsb_invalid={{"AU","12345678","1234567"},{"AU","12345678","12345"}};
    private String[][] aba_valid={{"US","12345678","1234567UH"},{"CN","12345678","123"},{"US","1",""}};
    private String[][] aba_invalid={{"US","12345678","1234567890"},{"US","12345678","qwertyui"},{"US","12345678",""}};
    @Test
    public void _payment_method_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'payment_method' field required, the value should be either 'LOCAL' or 'SWIFT'\"}", result.getContent());
    }

    @Test
    public void _payment_method_is_invalid() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"EAGLE\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'payment_method' field required, the value should be either 'LOCAL' or 'SWIFT'\"}", result.getContent());
    }

    @Test
    public void _payment_method_is_valid() {
        for (int i = 0; i < method.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"" + method[i] + "\",\n" +
                    "  \"bank_country_code\": \"US\",\n" +
                    "  \"account_name\": \"John Smith\",\n" +
                    "  \"account_number\": \"123\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }

    @Test
    public void _country_code_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"SWIFT\",\n" +
                "  \"bank_country_code\": \"\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'bank_country_code' is required, and should be one of 'US', 'AU', or 'CN'\"}", result.getContent());
    }

    @Test
    public void _country_code_is_valid() {
        for (int i = 0; i < country_code_invalid.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + country_code_invalid[i][0] + "\",\n" +
                    "  \"account_name\": \"John Smith\",\n" +
                    "  \"account_number\": \"" + country_code_invalid[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    " \"bsb\": \"123456\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }

    @Test
    public void _country_code_is_invalid() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"LOCAL\",\n" +
                "  \"bank_country_code\": \"eagle\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123456\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                " \"bsb\": \"123456\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'bank_country_code' is required, and should be one of 'US', 'AU', or 'CN'\"}", result.getContent());
    }

    @Test
    public void _account_name_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"SWIFT\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'account_name' is required\"}", result.getContent());
    }

    @Test
    public void _account_name_is_valid() {
        for (int i = 0; i < account_name_valid.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"SWIFT\",\n" +
                    "  \"bank_country_code\": \"US\",\n" +
                    "  \"account_name\": \"" + account_name_valid[i] + "\",\n" +
                    "  \"account_number\": \"123\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }

    @Test
    public void _account_name_is_invalid() {
        for (int i = 0; i < account_name_invalid.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"SWIFT\",\n" +
                    "  \"bank_country_code\": \"US\",\n" +
                    "  \"account_name\": \"" + account_name_invalid[i] + "\",\n" +
                    "  \"account_number\": \"123\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _400Verify(result);
            Assert.assertEquals("{\"error\":\"Length of account_name should be between 2 and 10\"}", result.getContent());
        }
    }
    @Test
    public void _account_number_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"SWIFT\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"eagle\",\n" +
                "  \"account_number\": \"\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'account_number' is required\"}", result.getContent());
    }
    @Test
    public void _swift_code_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"SWIFT\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'swift_code' is required when payment method is 'SWIFT'\"}", result.getContent());
    }
    //Local with null swift
    @Test
    public void _swift_code_is_mandatory02() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"LOCAL\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"John Smith\",\n" +
                "  \"account_number\": \"123\",\n" +
                "  \"swift_code\": \"\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _200Verify(result);
    }
    @Test
    public void _swift_country_valid() {
        verifySwiftCountry(swift_match_country_valid);
    }
    @Test
    public void _swift_country_invalid() {
        verifySwiftCountryInvalid(swift_match_country_invalid,"The swift code is not valid for the given bank country code");
    }
    @Test
    public void _swift_length_valid() {
        verifySwiftCountry(swift_length_valid);
    }
    @Test
    public void _swift_length_invalid() {
        verifySwiftCountryInvalid(swift_length_invalid,"{\"error\":\"Length of 'swift_code' should be either 8 or 11\"}");
    }
    @Test
    public void _us_account_number_is_valid() {
        verifyAccount(us_account_number_valid);
    }

    @Test
    public void _us_account_number_is_invalid() {
        verifyInvalidAccount(us_account_number_invalid, "{\"error\":\"Length of account_number should be between 1 and 17 when bank_country_code is 'US'\"}");
    }

    @Test
    public void _au_account_number_is_valid() {
        verifyAccount(au_account_number_valid);
    }

    @Test
    public void _au_account_number_is_invalid() {
        verifyInvalidAccount(au_account_number_invalid, "{\"error\":\"Length of account_number should be between 6 and 9 when bank_country_code is 'AU'\"}");
    }

    @Test
    public void _cn_account_number_is_valid() {
        verifyAccount(cn_account_number_valid);
    }

    @Test
    public void _cn_account_number_is_invalid() {
        verifyInvalidAccount(cn_account_number_invalid, "{\"error\":\"Length of account_number should be between 8 and 20 when bank_country_code is 'CN'\"}");
    }
    @Test
    public void _bsb_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"LOCAL\",\n" +
                "  \"bank_country_code\": \"AU\",\n" +
                "  \"account_name\": \"eagle\",\n" +
                "  \"account_number\": \"12345678\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"aba\": \"11122233A\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'bsb' is required when bank country code is 'AU'\"}",result.getContent());
    }
    @Test
    public void _bsb_is_valid() {
        verifyValidbsb(bsb_valid);
    }

    @Test
    public void _bsb_is_invalid() {
        verifyInvalidbsb(bsb_invalid,"{\"error\":\"Length of 'bsb' should be 6\"}");
    }
    @Test
    public void _aba_is_mandatory() {
        httpRequest.setParameter("{\n" +
                "  \"payment_method\": \"LOCAL\",\n" +
                "  \"bank_country_code\": \"US\",\n" +
                "  \"account_name\": \"eagle\",\n" +
                "  \"account_number\": \"12345678\",\n" +
                "  \"swift_code\": \"ICBCUSBJ\",\n" +
                "  \"bsb\": \"\"\n" +
                "}");
        result = httpRequest.getResponseByPostMethod();
        _400Verify(result);
        Assert.assertEquals("{\"error\":\"'aba' is required when bank country code is 'US'\"}",result.getContent());
    }
    @Test
    public void _aba_is_valid() {
        verifyValidaba(aba_valid);
    }
    @Test
    public void _aba_is_invalid() {
        verifyInvalidaba(aba_invalid,"{\"error\":\"Length of 'aba' should be 9\"}");
    }
    private void verifyAccount(String[][] account) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }
    private void verifySwiftCountry(String[][] account) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"SWIFT\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \""+account[i][2]+"\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }

    private void verifyInvalidAccount(String[][] account, String expected) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _400Verify(result);
            Assert.assertEquals(expected,result.getContent());
        }
    }
    private void verifySwiftCountryInvalid(String[][] account,String expected) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"SWIFT\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \""+account[i][2]+"\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _400Verify(result);
           Assert.assertThat(result.getContent(),org.hamcrest.CoreMatchers.containsString(expected));
        }
    }
    private void verifyInvalidbsb(String[][] account, String expected) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \""+account[i][2]+"\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _400Verify(result);
            Assert.assertEquals(expected,result.getContent());
        }
    }
    private void verifyValidbsb(String[][] account) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \""+account[i][2]+"\",\n" +
                    "  \"aba\": \"11122233A\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }
    private void verifyInvalidaba(String[][] account, String expected) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \""+account[0][2]+"\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _400Verify(result);
            Assert.assertEquals(expected,result.getContent());
        }
    }
    private void verifyValidaba(String[][] account) {
        for (int i = 0; i < account.length; i++) {
            httpRequest.setParameter("{\n" +
                    "  \"payment_method\": \"LOCAL\",\n" +
                    "  \"bank_country_code\": \"" + account[i][0] + "\",\n" +
                    "  \"account_name\": \"eagle\",\n" +
                    "  \"account_number\": \"" + account[i][1] + "\",\n" +
                    "  \"swift_code\": \"ICBCUSBJ\",\n" +
                    "  \"bsb\": \"123456\",\n" +
                    "  \"aba\": \""+account[0][2]+"\"\n" +
                    "}");
            result = httpRequest.getResponseByPostMethod();
            _200Verify(result);
        }
    }
    private void _400Verify(HttpClientResult result) {
        Assert.assertTrue("Response is null！", null != result);
        Assert.assertTrue("实际结果：" + result.getContent(), 400 == result.getCode());
    }

    private void _200Verify(HttpClientResult result) {
        Assert.assertTrue("Response is null！", null != result);
        Assert.assertTrue("实际结果：" + result.getContent(), 200 == result.getCode());
        Assert.assertEquals("{\"success\":\"Bank details saved\"}", result.getContent());
    }
}