package com.salesforce.testsuits.integrated;

import com.salesforce.base.BaseUiClass;
import com.salesforce.base.JsonOutput;
import com.salesforce.data.SalesForceDataBean;
import com.salesforce.pages.AccountsPage;
import com.salesforce.utilities.GenericUtility;
import io.restassured.response.Response;
import org.apache.log4j.Logger;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class Integrated  extends BaseUiClass {
    private static String id;
    private static String accountName;
    public Logger logger = Logger.getLogger(this.getClass());
    @Test(dataProvider = "excelData", description = "Create a new account from API and Validate from UI", priority = 0)
    public void createNewAccount(Map<String, String> tcData) {
        this.logger.info("INTEGRATED TEST STARTED!");
        //getting url from env properties file
        String baseUrl = (String)props.getProperty("env.baseurl");
        String endPoint = (String)props.getProperty("accounts.endpoint");
        //creating object of SalesForceDataBean class
        SalesForceDataBean salesforceData = new SalesForceDataBean();
        //getting random data for specified fields
        salesforceData.account.fillRandomData();
        salesforceData.account.fillDataFromDB("select name from accounts");
        //request type
        String requestType = "POST";
        //making Http call with REST Assured
        System.out.println("Creating an Account using REST API");
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Request Type: " + requestType);
        Response response = makeRestCallUsingRestAssured(baseUrl, endPoint, requestType, salesforceData.account, new HashMap<String, String>());
        this.logger.info("Response Code: " + response.getStatusCode());
        //storing id from json response
        id = response.jsonPath().get("id").toString();
        System.out.println("Verifying Account on Salesforce platform UI");
        //creating object of AccountsPage class
        AccountsPage accountsPage = new AccountsPage(driver);
        //calling openPage method from AccountsPage class
        accountsPage.openPage();
        //waiting for page to load
        GenericUtility.waitForPageToLoad();
        //searching account by accountName which was generated by Databean util
        accountsPage.getSearchField().sendKeys(salesforceData.account.Name, Keys.ENTER);
        //waiting for page to load
        GenericUtility.waitForPageToLoad();
        //assigning result of search
        String actualAccountName = accountsPage.getAccountName().getText();
        //doing assertions
        Assert.assertEquals(actualAccountName, salesforceData.account.Name);
        this.logger.info("Actual result is "+actualAccountName+" and expected result is "+salesforceData.account.Name);
        //calling logOut method from AccountsPage class
        accountsPage.logOut();
        System.out.println("Account creation successfully validated on the UI!");
    }

    @Test(dataProvider = "excelData", description = "Update created account from UI and validate from API", priority = 1)
    public void updatedAccount(Map<String, String> tcData){
        accountName = GenericUtility.getNewName();
        AccountsPage accountsPage = new AccountsPage(driver);
        AccountsPage.NewAccountPage newAccountPage = accountsPage.new NewAccountPage(driver);
        accountsPage.openPage();
        accountsPage.getAccountDetailsDropdown().click();
        GenericUtility.waitForPageToLoad();
        accountsPage.getEditBtn().click();
        GenericUtility.waitForPageToLoad();
        newAccountPage.getNewAccountName().clear();
        newAccountPage.getNewAccountName().sendKeys(accountName);
        newAccountPage.getSaveBtn().click();
        accountsPage.logOut();
        String targetURL = manifestJsonObject.get(tcData.get("URL")) + "/account/"+id;
        String requestType = tcData.get("Request Type");
        JsonOutput jsonResponse = makeRestCallUsingHttpClient(targetURL, requestType);
        System.out.println("Response:\n" + jsonResponse.getJsonResponse());
        String actualName = jsonResponse.getJsonResponse().get("Name").toString();
        Assert.assertEquals(actualName, accountName);
    }

    @Test(dataProvider = "excelData", description = "Delete account from API and validate from UI", priority = 2)
    public void deleteAccount(Map<String, String> tcData) {
        String targetURL = manifestJsonObject.get(tcData.get("URL")) + "/account/"+id;
        String requestType = tcData.get("Request Type");
        makeRestCallUsingHttpClient(targetURL, requestType);
        String expectedSearchResult = "No items to display.";
        AccountsPage accountsPage = new AccountsPage(driver);
        accountsPage.openPage();
        GenericUtility.waitForPageToLoad();
        accountsPage.getSearchField().sendKeys(accountName, Keys.ENTER);
        GenericUtility.waitForPageToLoad();
        String actualSearchResult = accountsPage.getNoItemsToDisplay().getText();
        Assert.assertEquals(actualSearchResult, expectedSearchResult);
        accountsPage.logOut();
    }
}
