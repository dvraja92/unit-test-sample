package services;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import keynectup.Global;
import models.Profile;
import models.Role;
import models.SentCard;
import models.SmsMessage;
import models.SmsMessageType;
import models.SummaryEmail;
import models.SummaryType;
import models.User;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import play.Logger;
import utils.logger.LoggerBuffer;
import base.BaseTest;
import base.TestCallback;

/**
 * Test the sent cart service class
 * @author dvraja
 * 
 */
public class SentCardServiceTest extends BaseTest {
	
	private SentCardService sentCardService;
	
	private ProfileService profileService;
	
	private UserService userService;
	
	private SummaryEmailService summaryEmailService;
	
	private SmsMessageService smsMessageService;
	
	/**
	 * Create a random sent card for this user
	 * @param sentCardService
	 * @param user
	 */
	private SentCard createSentCard(User user) {
		final List<Profile> profiles = profileService.findAllProfile(user).toResult();
		
		final SentCard sentCard = new SentCard();
		sentCard.setUser(user);
		sentCard.setProfile(profiles.get(0));
		sentCard.setFullName("Dummy User " + RandomStringUtils.randomAlphabetic(5));
		sentCard.setEmailAddress("dummy"+ RandomStringUtils.randomAlphabetic(5) +"@decipherzon.com");
		sentCard.setPhoneNumber("+63 " + RandomStringUtils.randomNumeric(10));
		sentCard.setNotes("Test notes");
		sentCard.setDateSent(new DateTime().toDate());
		sentCardService.save(sentCard);
		
		// subtract by 1 day
		final SentCard toUpdate = sentCardService.findByPk(sentCard.getId());
		toUpdate.setCreatedDate(new DateTime().minusHours(22).toDate());
		toUpdate.setUpdatedDate(new DateTime().minusHours(22).toDate());
		sentCardService.update(toUpdate);
		
		return toUpdate;
	}
	
	/**
	 * Get sms message for this sent card
	 * @param sentCard
	 */
	private void createSmsMessage(SentCard sentCard, SmsMessageType smsMessagType, String status) {
		
		final SmsMessage smsMessage = new SmsMessage();
		smsMessage.setRecipient("+63 917649009350");
		smsMessage.setUser(sentCard.getUser());
		smsMessage.setMessage("Hello world of unit test");
		smsMessage.setMessageStatus(status);
		smsMessage.setTargetObjectId(sentCard.getId());
		smsMessage.setSmsMessageType(smsMessagType);
		smsMessage.setErrorCode("somecode");
		
		smsMessageService.save(smsMessage);
	}
	

//	@Test
	public void testSendDailySummaryPhilippines() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				// get the beans that we need
				sentCardService = Global.getInstance().getSpringContext().getBean(SentCardService.class);
				assertThat(sentCardService).isNotNull();
				
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				profileService = Global.getInstance().getSpringContext().getBean(ProfileService.class);
				assertThat(profileService).isNotNull();
				
				summaryEmailService = Global.getInstance().getSpringContext().getBean(SummaryEmailService.class);
				assertThat(summaryEmailService).isNotNull();
				
				Logger.debug("Users: " + userService.findAll().toResult().size());
				
				// add a new user
				userService.createDummyUser("dchrstall", "password", Role.NORMAL, 240, true);
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				
				// make sure we don't have any users before populating
				assertThat(userService.count()).isEqualTo(2);
				assertThat(profileService.count()).isEqualTo(4);
				
				// make sure that the number of offsets are correct
				final List<Integer> timezoneOffsets = userService.findAllAvailableTimezoneOffset();
				assertThat(timezoneOffsets.size()).isEqualTo(4);
				
				// set the time to 11pm
				final DateTimeZone tz = DateTimeZone.forOffsetHours(480 / 60);
				final DateTime phLocalDt = new DateTime().withZone(tz);
				final DateTime phStartDt = new DateTime(phLocalDt.getYear(), phLocalDt.getMonthOfYear(), phLocalDt.getDayOfMonth(), 0, 0 ,0, tz);
				final LocalDateTime utcLocalDt = new LocalDateTime(phStartDt.getMillis());
				
				// test if send daily summary at 11pm. it should not email anybody
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusHours(22).toDateTime().getMillis());
				
				// create the sent cards after the new date is set
				final User dchrystallUser = userService.findByUsername("dchrstall");
				final User dvrajaUser = userService.findByUsername("dvraja");
				
				createSentCard(dchrystallUser);
				createSentCard(dchrystallUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				assertThat(sentCardService.count()).isEqualTo(6);
				
				// check and make sure we don't send it at 11pm
				int totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(0);
				
				// send the cards at midnight ph time
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusDays(1).toDateTime().getMillis());
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(1);
				
				// when send daily summary is executed again, it shouldn't email anybody
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(0);
				
				// check and make sure that the first record is user dvraja and has 4 cards sent
				SummaryEmail summaryEmail = summaryEmailService.findByPk(1L);
				assertThat(summaryEmail.getSummaryType()).isEqualTo(SummaryType.DAILY);
				assertThat(summaryEmail.getUser()).isEqualTo(dvrajaUser);
				assertThat(summaryEmail.getCardsIncludedOnEmail()).isEqualTo(4);
				
				// now adjust date to next day to test that this will work the next day
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusDays(8).toDateTime().getMillis());
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(1);
				
				// check and make sure that the first record is user dvraja and has 0 cards
				summaryEmail = summaryEmailService.findByPk(2L);
				assertThat(summaryEmail.getSummaryType()).isEqualTo(SummaryType.DAILY);
				assertThat(summaryEmail.getUser()).isEqualTo(dvrajaUser);
				assertThat(summaryEmail.getCardsIncludedOnEmail()).isEqualTo(0);
			}
		});
	}

	
//	@Test
	public void testSendDailySummaryBostonUsa() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				// get the beans that we need
				sentCardService = Global.getInstance().getSpringContext().getBean(SentCardService.class);
				assertThat(sentCardService).isNotNull();
				
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				profileService = Global.getInstance().getSpringContext().getBean(ProfileService.class);
				assertThat(profileService).isNotNull();
				
				summaryEmailService = Global.getInstance().getSpringContext().getBean(SummaryEmailService.class);
				assertThat(summaryEmailService).isNotNull();
				
				
				Logger.debug("Users: " + userService.findAll().toResult().size());
				
				// add a new users
				userService.createDummyUser("dchrstall", "password", Role.NORMAL, 240, true);
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				
				// make sure we don't have any users before populating
				assertThat(userService.count()).isEqualTo(2);
				assertThat(profileService.count()).isEqualTo(4);
				
				// make sure that the number of offsets are correct
				final List<Integer> timezoneOffsets = userService.findAllAvailableTimezoneOffset();
				assertThat(timezoneOffsets.size()).isEqualTo(4);
				
				// set the time to 11pm
				final DateTimeZone tz = DateTimeZone.forOffsetHours(-240 / 60);
				final DateTime phLocalDt = new DateTime().withZone(tz);
				final DateTime phStartDt = new DateTime(phLocalDt.getYear(), phLocalDt.getMonthOfYear(), phLocalDt.getDayOfMonth(), 0, 0 ,0, tz);
				final LocalDateTime utcLocalDt = new LocalDateTime(phStartDt.getMillis());
				
				// test if send daily summary at 11pm. it should not email anybody
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusHours(22).toDateTime().getMillis());
				
				// create the sent cards after the new date is set
				final User dchrystallUser = userService.findByUsername("dchrstall");
				final User dvrajaUser = userService.findByUsername("dvraja");
				
				createSentCard(dchrystallUser);
				createSentCard(dchrystallUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				createSentCard(dvrajaUser);
				assertThat(sentCardService.count()).isEqualTo(6);
				
				// check and make sure we don't send it at 11pm
				int totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(0);
				
				// send the cards at midnight ph time
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusDays(1).toDateTime().getMillis());
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(1);
				
				// when send daily summary is executed again, it shouldn't email anybody
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(0);
				
				// check and make sure that the first record is user dvraja and has 4 cards sent
				SummaryEmail summaryEmail = summaryEmailService.findByPk(1L);
				assertThat(summaryEmail.getSummaryType()).isEqualTo(SummaryType.DAILY);
				assertThat(summaryEmail.getUser()).isEqualTo(dchrystallUser);
				assertThat(summaryEmail.getCardsIncludedOnEmail()).isEqualTo(2);
				
				// now adjust date to next day to test that this will work the next day
				DateTimeUtils.setCurrentMillisFixed(utcLocalDt.plusDays(8).toDateTime().getMillis());
				totalUsersEmailed = sentCardService.sendDailySummary();
				assertThat(totalUsersEmailed).isEqualTo(1);
				
				// check and make sure that the first record is user dvraja and has 0 cards
				summaryEmail = summaryEmailService.findByPk(2L);
				assertThat(summaryEmail.getSummaryType()).isEqualTo(SummaryType.DAILY);
				assertThat(summaryEmail.getUser()).isEqualTo(dchrystallUser);
				assertThat(summaryEmail.getCardsIncludedOnEmail()).isEqualTo(0);
			}
		});
	}
	
	@Test
	public void testNotifyUndeliveredCards() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				// get the beans that we need
				sentCardService = Global.getInstance().getSpringContext().getBean(SentCardService.class);
				assertThat(sentCardService).isNotNull();
				
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				profileService = Global.getInstance().getSpringContext().getBean(ProfileService.class);
				assertThat(profileService).isNotNull();
				
				summaryEmailService = Global.getInstance().getSpringContext().getBean(SummaryEmailService.class);
				assertThat(summaryEmailService).isNotNull();
				
				smsMessageService = Global.getInstance().getSpringContext().getBean(SmsMessageService.class);
				assertThat(smsMessageService).isNotNull();
				
				Logger.debug("Users: " + userService.findAll().toResult().size());
				
				// add a new user
				userService.createDummyUser("dchrstall", "password", Role.NORMAL, 240, true);
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				
				// make sure we don't have any users before populating
				assertThat(userService.count()).isEqualTo(2);
				assertThat(profileService.count()).isEqualTo(4);
				
				// make sure that the number of offsets are correct
				final List<Integer> timezoneOffsets = userService.findAllAvailableTimezoneOffset();
				assertThat(timezoneOffsets.size()).isEqualTo(4);
				
				// create the sent cards after the new date is set
				final User dchrystallUser = userService.findByUsername("dchrstall");
				final User dvrajaUser = userService.findByUsername("dvraja");
				
				final SentCard sentCard1 = createSentCard(dchrystallUser);
				final SentCard sentCard2 = createSentCard(dchrystallUser);
				final SentCard sentCard3 = createSentCard(dvrajaUser);
				final SentCard sentCard4 = createSentCard(dvrajaUser);
				final SentCard sentCard5 = createSentCard(dvrajaUser);
				final SentCard sentCard6 = createSentCard(dvrajaUser);
				assertThat(sentCardService.count()).isEqualTo(6);
				
				// create the sms
				createSmsMessage(sentCard1, SmsMessageType.VERIFICATION, SmsMessageService.STATUS_DELIVERED);
				createSmsMessage(sentCard2, SmsMessageType.SEND_PROFILE_SECOND, SmsMessageService.STATUS_NEW);
				createSmsMessage(sentCard3, SmsMessageType.SEND_PROFILE, SmsMessageService.STATUS_NEW);
				createSmsMessage(sentCard4, SmsMessageType.SEND_PROFILE, SmsMessageService.STATUS_SENT);
				createSmsMessage(sentCard5, SmsMessageType.RESEND_PROFILE, SmsMessageService.STATUS_SENT);
				createSmsMessage(sentCard6, SmsMessageType.VERIFICATION, SmsMessageService.STATUS_SENT);
				assertThat(smsMessageService.count()).isEqualTo(6);
				
				List<SmsMessage> idledSmsMessages = smsMessageService.findIdledSmsMessages(SmsMessageService.SMS_MESSAGE_MAX_IDLE_TIME_IN_MINUTES, 
						SmsMessageType.SEND_PROFILE, SmsMessageType.RESEND_PROFILE);
				
				// this should be zero since we haven't moved time yet
				assertThat(idledSmsMessages.size()).isEqualTo(0);
				
				// now, let's try moving date by 5 minutes
				DateTimeUtils.setCurrentMillisFixed(DateTime.now().plusMinutes(5).getMillis());
				
				idledSmsMessages = smsMessageService.findIdledSmsMessages(SmsMessageService.SMS_MESSAGE_MAX_IDLE_TIME_IN_MINUTES, 
						SmsMessageType.SEND_PROFILE, SmsMessageType.RESEND_PROFILE);
				
				// this should still be zero since we haven't more than 15 minutes
				assertThat(idledSmsMessages.size()).isEqualTo(0);
				
				// now, let's try moving date by 15 minutes
				DateTimeUtils.setCurrentMillisFixed(DateTime.now().plusMinutes(16).getMillis());
				
				idledSmsMessages = smsMessageService.findIdledSmsMessages(SmsMessageService.SMS_MESSAGE_MAX_IDLE_TIME_IN_MINUTES, 
						SmsMessageType.SEND_PROFILE, SmsMessageType.RESEND_PROFILE);
				
				// this should now be 3
				assertThat(idledSmsMessages.size()).isEqualTo(3);
				
				// now try to 
				final LoggerBuffer loggerBuffer = new LoggerBuffer();
				assertThat(sentCardService.notifyUndeliveredSentCards(loggerBuffer)).isEqualTo(3);
				
				// check if all idled messages changed forced stop
				idledSmsMessages = smsMessageService.findIdledSmsMessages(SmsMessageService.SMS_MESSAGE_MAX_IDLE_TIME_IN_MINUTES, 
						SmsMessageType.SEND_PROFILE, SmsMessageType.RESEND_PROFILE);
				assertThat(sentCardService.notifyUndeliveredSentCards(loggerBuffer)).isEqualTo(0);
			}
		});
	}
	
}
