package myblog.services;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class EmailService {
    private static final Properties config = loadConfig();
    private static final String FROM_EMAIL = config.getProperty("smtp.username");
    private static final String PASSWORD = config.getProperty("smtp.password");
    private static final String SMTP_HOST = config.getProperty("smtp.host");
    private static final String SMTP_PORT = config.getProperty("smtp.port");

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream input = EmailService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Warning: config.properties not found in classpath");
                throw new RuntimeException("Unable to find config.properties");
            }
            props.load(input);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Error loading config.properties", e);
        }
    }

    public static String sendVerificationEmail(String toEmail, String username) {
        String verificationToken = UUID.randomUUID().toString();
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        System.out.println("Attempting to establish SMTP connection to: " + SMTP_HOST + ":" + SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Verify your email address");

            String verificationLink = String.format("http://localhost:8080/verify?token=%s&username=%s",
                    verificationToken, username);

            String htmlContent = String.format("""
                <html>
                <body>
                    <h2>Welcome to My Blog!</h2>
                    <p>Hi %s,</p>
                    <p>Please click the link below to verify your email address:</p>
                    <a href="%s">Verify Email</a>
                    <p>If you didn't create an account, you can ignore this email.</p>
                </body>
                </html>
                """, username, verificationLink);

            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            System.out.println("Attempting to send email to: " + toEmail);
            Transport.send(message);
            System.out.println("Email successfully sent to: " + toEmail);
            
            return verificationToken;
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + toEmail + ". Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 