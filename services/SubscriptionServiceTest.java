package services;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import keynectup.Global;
import models.AccountType;
import models.PaymentPlatform;
import models.Role;
import models.Subscription;
import models.User;
import models.forms.CreateSubscriptionForm;

import org.junit.Test;

import play.data.Form;
import base.BaseTest;
import base.TestCallback;
import exceptions.SubscriptionException;

public class SubscriptionServiceTest extends BaseTest {

	private UserService userService;
	
	private SubscriptionService subscriptionService;
	
	private static final String RECEIPT_NUMBER = "ewoJInNpZ25hdHVyZSIgPSAiQWh1bWxmUFY0bUdXTW9FZHB6dDJzNGlkRjNOZGpyWHd4L2F6 OWw2blV2dklPSE1sWGFaZ0l3RWpaR0I4YWVkRGtaaW9UaXhYUzh1VmNNMW1tZk4zNnN0VmJG bUUzZjJmcEJ1M3JLVHJXMXlpbnRJT2h2ZWF5RHk5OVNKRXkyK0xJelI4WXRNUTVEWFhQdCtV MmdNSTdMNWd5SG1SbVEweU05QjFTU0lyV0VSOUFBQURWekNDQTFNd2dnSTdvQU1DQVFJQ0NC dXA0K1BBaG0vTE1BMEdDU3FHU0liM0RRRUJCUVVBTUg4eEN6QUpCZ05WQkFZVEFsVlRNUk13 RVFZRFZRUUtEQXBCY0hCc1pTQkpibU11TVNZd0pBWURWUVFMREIxQmNIQnNaU0JEWlhKMGFX WnBZMkYwYVc5dUlFRjFkR2h2Y21sMGVURXpNREVHQTFVRUF3d3FRWEJ3YkdVZ2FWUjFibVZ6 SUZOMGIzSmxJRU5sY25ScFptbGpZWFJwYjI0Z1FYVjBhRzl5YVhSNU1CNFhEVEUwTURZd056 QXdNREl5TVZvWERURTJNRFV4T0RFNE16RXpNRm93WkRFak1DRUdBMVVFQXd3YVVIVnlZMmho YzJWU1pXTmxhWEIwUTJWeWRHbG1hV05oZEdVeEd6QVpCZ05WQkFzTUVrRndjR3hsSUdsVWRX NWxjeUJUZEc5eVpURVRNQkVHQTFVRUNnd0tRWEJ3YkdVZ1NXNWpMakVMTUFrR0ExVUVCaE1D VlZNd2daOHdEUVlKS29aSWh2Y05BUUVCQlFBRGdZMEFNSUdKQW9HQkFNbVRFdUxnamltTHdS Snh5MW9FZjBlc1VORFZFSWU2d0Rzbm5hbDE0aE5CdDF2MTk1WDZuOTNZTzdnaTNvclBTdXg5 RDU1NFNrTXArU2F5Zzg0bFRjMzYyVXRtWUxwV25iMzRucXlHeDlLQlZUeTVPR1Y0bGpFMU93 QytvVG5STStRTFJDbWVOeE1iUFpoUzQ3VCtlWnRERWhWQjl1c2szK0pNMkNvZ2Z3bzdBZ01C QUFHamNqQndNQjBHQTFVZERnUVdCQlNKYUVlTnVxOURmNlpmTjY4RmUrSTJ1MjJzc0RBTUJn TlZIUk1CQWY4RUFqQUFNQjhHQTFVZEl3UVlNQmFBRkRZZDZPS2RndElCR0xVeWF3N1hRd3VS V0VNNk1BNEdBMVVkRHdFQi93UUVBd0lIZ0RBUUJnb3Foa2lHOTJOa0JnVUJCQUlGQURBTkJn a3Foa2lHOXcwQkFRVUZBQU9DQVFFQWVhSlYyVTUxcnhmY3FBQWU1QzIvZkVXOEtVbDRpTzRs TXV0YTdONlh6UDFwWkl6MU5ra0N0SUl3ZXlOajVVUllISytIalJLU1U5UkxndU5sMG5rZnhx T2JpTWNrd1J1ZEtTcTY5Tkluclp5Q0Q2NlI0Szc3bmI5bE1UQUJTU1lsc0t0OG9OdGxoZ1Iv MWtqU1NSUWNIa3RzRGNTaVFHS01ka1NscDRBeVhmN3ZuSFBCZTR5Q3dZVjJQcFNOMDRrYm9p SjNwQmx4c0d3Vi9abEwyNk0ydWVZSEtZQ3VYaGRxRnd4VmdtNTJoM29lSk9PdC92WTRFY1Fx N2VxSG02bTAzWjliN1BSellNMktHWEhEbU9Nazd2RHBlTVZsTERQU0dZejErVTNzRHhKemVi U3BiYUptVDdpbXpVS2ZnZ0VZN3h4ZjRjemZIMHlqNXdOelNHVE92UT09IjsKCSJwdXJjaGFz ZS1pbmZvIiA9ICJld29KSW05eWFXZHBibUZzTFhCMWNtTm9ZWE5sTFdSaGRHVXRjSE4wSWlB OUlDSXlNREUwTFRBM0xUTXdJREEzT2pVMU9qQXhJRUZ0WlhKcFkyRXZURzl6WDBGdVoyVnNa WE1pT3dvSkluQjFjbU5vWVhObExXUmhkR1V0YlhNaUlEMGdJakUwTURZM05UTTBNak16TlRR aU93b0pJblZ1YVhGMVpTMXBaR1Z1ZEdsbWFXVnlJaUE5SUNJeE1qY3laV016TVRrNE9URXhO akU0TkdZNU16YzJZelZtWlRneE1qVm1NbU0xWTJKak1ESTFJanNLQ1NKdmNtbG5hVzVoYkMx MGNtRnVjMkZqZEdsdmJpMXBaQ0lnUFNBaU1UQXdNREF3TURFeE9EVTRPVGcwTXlJN0Nna2la WGh3YVhKbGN5MWtZWFJsSWlBOUlDSXhOREEyTnpVek56SXpNelUwSWpzS0NTSjBjbUZ1YzJG amRHbHZiaTFwWkNJZ1BTQWlNVEF3TURBd01ERXhPRFl5TWpBeE5DSTdDZ2tpYjNKcFoybHVZ V3d0Y0hWeVkyaGhjMlV0WkdGMFpTMXRjeUlnUFNBaU1UUXdOamN6TWpFd01UQXdNQ0k3Q2dr aWQyVmlMVzl5WkdWeUxXeHBibVV0YVhSbGJTMXBaQ0lnUFNBaU1UQXdNREF3TURBeU9EUTFO ak0xT1NJN0Nna2lZblp5Y3lJZ1BTQWlNUzR3TGpFaU93b0pJblZ1YVhGMVpTMTJaVzVrYjNJ dGFXUmxiblJwWm1sbGNpSWdQU0FpTmpORlJVTkNNakl0TmpWQ01DMDBNekU0TFRoRU5rRXRN RGt5UkRaR09VSkRPVGM0SWpzS0NTSmxlSEJwY21WekxXUmhkR1V0Wm05eWJXRjBkR1ZrTFhC emRDSWdQU0FpTWpBeE5DMHdOeTB6TUNBeE16bzFOVG95TXlCQmJXVnlhV05oTDB4dmMxOUJi bWRsYkdWeklqc0tDU0pwZEdWdExXbGtJaUE5SUNJNU1EUTJNVFl3TXpVaU93b0pJbVY0Y0ds eVpYTXRaR0YwWlMxbWIzSnRZWFIwWldRaUlEMGdJakl3TVRRdE1EY3RNekFnTWpBNk5UVTZN ak1nUlhSakwwZE5WQ0k3Q2draWNISnZaSFZqZEMxcFpDSWdQU0FpVUhKbGJXbDFiVk4xWW5O amNtbHdkR2x2YmxOMFlXZHBibWNpT3dvSkluQjFjbU5vWVhObExXUmhkR1VpSUQwZ0lqSXdN VFF0TURjdE16QWdNakE2TlRBNk1qTWdSWFJqTDBkTlZDSTdDZ2tpYjNKcFoybHVZV3d0Y0hW eVkyaGhjMlV0WkdGMFpTSWdQU0FpTWpBeE5DMHdOeTB6TUNBeE5EbzFOVG93TVNCRmRHTXZS MDFVSWpzS0NTSmlhV1FpSUQwZ0ltTnZiUzVyWlhsdVpXTjBkWEF1YVc5emMzUmhaMmx1WnlJ N0Nna2ljSFZ5WTJoaGMyVXRaR0YwWlMxd2MzUWlJRDBnSWpJd01UUXRNRGN0TXpBZ01UTTZO VEE2TWpNZ1FXMWxjbWxqWVM5TWIzTmZRVzVuWld4bGN5STdDZ2tpY1hWaGJuUnBkSGtpSUQw Z0lqRWlPd3A5IjsKCSJlbnZpcm9ubWVudCIgPSAiU2FuZGJveCI7CgkicG9kIiA9ICIxMDAi OwoJInNpZ25pbmctc3RhdHVzIiA9ICIwIjsKfQ==";
	
	//@Test
	public void testUpgradeToPremium() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				// get the beans that we need
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				subscriptionService = Global.getInstance().getSpringContext().getBean(SubscriptionService.class);
				assertThat(userService).isNotNull();
				
				// create dummy user
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				assertThat(userService.count()).isEqualTo(1);
				
				// create the mock data on form
				final Map<String, String> formParams = new HashMap<String, String>();
				formParams.put("receiptNumber", RECEIPT_NUMBER);
				formParams.put("name", "PremiumSubscriptionDev");
				formParams.put("platform", "IOS");
				
				final Form<CreateSubscriptionForm> subscriptionForm = new Form<CreateSubscriptionForm>(CreateSubscriptionForm.class).bind(formParams);
				User currentUser = userService.findByUsername("dvraja");
				
				// test if subscription is created properly
				try {
					final Subscription createdSubscription = subscriptionService.upgradeToPremium(currentUser, subscriptionForm);
					assertThat(createdSubscription).isNotNull();
				} catch (SubscriptionException e) {
					// TODO Auto-generated catch block
					assertThat(e).isNull();
				}
				assertThat(subscriptionService.count()).isEqualTo(1L);
				
				// just pass the same params and see if subscription is still 1
				try {
					final Subscription createdSubscription = subscriptionService.upgradeToPremium(currentUser, subscriptionForm);
					assertThat(createdSubscription).isNotNull();
				} catch (SubscriptionException e) {
					// TODO Auto-generated catch block
					assertThat(e).isNull();
				}
				assertThat(subscriptionService.count()).isEqualTo(1L);
				
				// check if this user's account type is now premium
				currentUser = userService.findByUsername("dvraja");
				assertThat(currentUser.getAccountType()).isEqualTo(AccountType.PREMIUM);
			}
		});
	}
	
	
	@Test
	public void testVerifyReceipt() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				// get the beans that we need
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				subscriptionService = Global.getInstance().getSpringContext().getBean(SubscriptionService.class);
				assertThat(userService).isNotNull();
				
				// create dummy user
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				assertThat(userService.count()).isEqualTo(1);
				
				// create the mock data on form
				final Map<String, String> formParams = new HashMap<String, String>();
				formParams.put("receiptNumber", RECEIPT_NUMBER);
				formParams.put("name", "PremiumSubscriptionDev");
				formParams.put("platform", "IOS");
				
				final Form<CreateSubscriptionForm> subscriptionForm = new Form<CreateSubscriptionForm>(CreateSubscriptionForm.class).bind(formParams);
				User currentUser = userService.findByUsername("dvraja");
				
				// test if subscription is created properly
				Subscription createdSubscription = null;
				try {
					createdSubscription = subscriptionService.upgradeToPremium(currentUser, subscriptionForm);
					assertThat(createdSubscription).isNotNull();
					assertThat(createdSubscription.getPlatform()).isEqualTo(PaymentPlatform.IOS);
				} catch (SubscriptionException e) {
					// TODO Auto-generated catch block
					assertThat(e).isNull();
				}
				assertThat(subscriptionService.count()).isEqualTo(1L);
				
				// now verify the receipt
				final Boolean verifyResult = subscriptionService.verifyIOSReceipt(createdSubscription);
				assertThat(verifyResult).isEqualTo(!createdSubscription.getExpired());
				
				// check if user account status was changed
				currentUser = userService.findByUsername("dvraja");
				
				if(createdSubscription.getExpired().equals(Boolean.TRUE)) {
					assertThat(currentUser.getAccountType()).isEqualTo(AccountType.FREE);
				}
				else {
					assertThat(currentUser.getAccountType()).isEqualTo(AccountType.PREMIUM);
				}
			}
		});
	}
}
