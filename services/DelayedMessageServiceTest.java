package services;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import base.BaseTest;
import base.TestCallback;
import keynectup.Global;
import models.DelayedMessage;
import models.DelayedMessageType;
import models.Role;
import models.SmsMessageType;
import models.User;
import models.wrappers.DelayedEmailMessageWrapper;
import models.wrappers.DelayedSmsMessageWrapper;
import play.Logger;
import utils.logger.LoggerBuffer;

/**
 * Test the sent cart service class
 * @author dvraja
 * 
 */
public class DelayedMessageServiceTest extends BaseTest {
	
	private UserService userService;
	
	private DelayedMessageService delayedMesageService;
	
	private SmsMessageService smsMessageService;

	@Test
	public void testFindDelayedMessagesToSend() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				delayedMesageService = Global.getInstance().getSpringContext().getBean(DelayedMessageService.class);
				assertThat(delayedMesageService).isNotNull();
				
				smsMessageService = Global.getInstance().getSpringContext().getBean(SmsMessageService.class);
				assertThat(smsMessageService).isNotNull();
				
				Logger.debug("Users: " + userService.findAll().toResult().size());
				
				// add a new user
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				
				// make sure we don't have any users before populating
				assertThat(userService.count()).isEqualTo(1);
				
				final User userDvraja = userService.findByUsername("dvraja");
				assertThat(userDvraja).isNotNull();
				
				// create the delayed sms
				final DelayedSmsMessageWrapper smsWrapper = new DelayedSmsMessageWrapper();
				smsWrapper.setSmsMessageType(SmsMessageType.SEND_PROFILE_SECOND);
				smsWrapper.setPhoneNumber("+63 9176490093");
				smsWrapper.setMessage("This is a test message");
				
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 0, smsWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 0, smsWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 60, smsWrapper);
				
				// create the delayed emails
				final DelayedEmailMessageWrapper emailWrapper = new DelayedEmailMessageWrapper();
				emailWrapper.setRecipient("rajad@decipherzone.com");
				emailWrapper.setFrom(MailService.EMAIL_NO_REPLY);
				emailWrapper.setSubject("Automated Message");
				emailWrapper.setBody("Some automated message");
				
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_EMAIL, 0, emailWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_EMAIL, 180, emailWrapper);
				
				assertThat(delayedMesageService.count()).isEqualTo(5);
				
				
				// check if we have only two delayed message to send
				final List<DelayedMessage> delayedMessageToSend = delayedMesageService.findDelayedMessagesToSend();
				assertThat(delayedMessageToSend.size()).isEqualTo(3);
				
				// check that there are 2 sms message and 1 email message
				int smsMessageCount = 0;
				int emailMessageCount = 0;
				
				for(DelayedMessage dm : delayedMessageToSend) {
					if(dm.getDelayedMessageType().equals(DelayedMessageType.DELAYED_EMAIL)) {
						emailMessageCount += 1;
					}
					else if(dm.getDelayedMessageType().equals(DelayedMessageType.DELAYED_SMS)) {
						smsMessageCount += 1;
					}
				}
				
				assertThat(emailMessageCount).isEqualTo(1);
				assertThat(smsMessageCount).isEqualTo(2);
			}
		});
	}
	 
	@Test
	public void testSendDelayedMessages() {
		runInFakeApplication(new TestCallback() {
			
			@Override
			public void execute() {	
				userService = Global.getInstance().getSpringContext().getBean(UserService.class);
				assertThat(userService).isNotNull();
				
				delayedMesageService = Global.getInstance().getSpringContext().getBean(DelayedMessageService.class);
				assertThat(delayedMesageService).isNotNull();
				
				smsMessageService = Global.getInstance().getSpringContext().getBean(SmsMessageService.class);
				assertThat(smsMessageService).isNotNull();
				
				Logger.debug("Users: " + userService.findAll().toResult().size());
				
				// add a new user
				userService.createDummyUser("dvraja", "password", Role.NORMAL, -480, true);
				
				// make sure we don't have any users before populating
				assertThat(userService.count()).isEqualTo(1);
				
				final User userDvraja = userService.findByUsername("dvraja");
				assertThat(userDvraja).isNotNull();
				
				
				// create the delayed sms
				final DelayedSmsMessageWrapper smsWrapper = new DelayedSmsMessageWrapper();
				smsWrapper.setSmsMessageType(SmsMessageType.SEND_PROFILE_SECOND);
				smsWrapper.setPhoneNumber("+63 9176490093");
				smsWrapper.setMessage("This is a test message");
				
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 0, smsWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 0, smsWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 0, smsWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_SMS, 120, smsWrapper);
				
				
				// create the delayed emails
				final DelayedEmailMessageWrapper emailWrapper = new DelayedEmailMessageWrapper();
				emailWrapper.setRecipient("rajad@decipherzone.com");
				emailWrapper.setFrom(MailService.EMAIL_NO_REPLY);
				emailWrapper.setSubject("Automated Message");
				emailWrapper.setBody("Some automated message");
				
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_EMAIL, 0, emailWrapper);
				delayedMesageService.createDelayedMessage(DelayedMessageType.DELAYED_EMAIL, 180, emailWrapper);
				
				assertThat(delayedMesageService.count()).isEqualTo(6);
				
				
				// check if we have only two delayed message to send
				final List<DelayedMessage> delayedMessageToSend = delayedMesageService.findDelayedMessagesToSend();
				assertThat(delayedMessageToSend.size()).isEqualTo(4);
				
				// now send it
				final LoggerBuffer lb = new LoggerBuffer();
				delayedMesageService.sendDelayedMessages(lb);
				assertThat(lb.getLineCount()).isNotEqualTo(0);
				
				
				// count the number of delayed message that was sent
				assertThat(delayedMesageService.countSent()).isEqualTo(4);
				assertThat(delayedMesageService.countUnsent()).isEqualTo(2);
				
				// assert if sms message is stored to db
				final Long smsMessageCount = smsMessageService.count();
				Logger.debug("smsMessageCount: " + smsMessageCount);
				assertThat(smsMessageCount).isEqualTo(3);
			}
		});
	}
}
