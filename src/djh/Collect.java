package djh;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tomcat.util.http.parser.Authorization;
import org.apache.tomcat.util.net.URL;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuth2Authorization;
import twitter4j.auth.OAuth2Token;
import twitter4j.auth.RequestToken;
import twitter4j.conf.PropertyConfiguration;

public class Collect {
	static final int API_KEY = 1, CONSUMER_KEY = 2, CONSUMER_SECRET = 4, TOKEN = 8, TOKEN_TYPE = 16, ALL_KEYS = 31;
	String userid = "djh_cs3250@yahoo.com", password = "djh3250djh3250",
			url = "https://api.twitter.com/1.1/search/tweets.json",
			filterUrl = "https://stream.twitter.com/1.1/statuses/filter.json",
			requestParam = "?track=\"marvel, dc commics\"", apiKey = "", consumerKey = "", consumerSecret = "",
			token = "", tokenSecret = "", apiK = "apiKey", consumer = "oauth.consumerKey",
			consumerSec = "oauth.consumerSecret", aToken = "oauth.accessToken",
			aTokenSecret = "oauth.accessTokenSecret", app_only_authentication = "https://api.twitter.com/oauth2/token",
			request_token_URL = "https://api.twitter.com/oauth/request_token",
			authorize_URL = "https://api.twitter.com/oauth/authorize",
			access_token_URL = "https://api.twitter.com/oauth/access_token";

	String consumerKeyName = "oauth.consumerKey";
	String consumerSecretName = "oauth.consumerSecret";
	String apiKeyName = "apiKey";

	OAuth2Token oAuth2Token;
	Twitter twitter;
	File file;
	InputStream inStream;
	OutputStream outStream;
	FileInputStream fis;
	FileOutputStream fr;
	URL twitUrl;
	RequestToken requestToken;
	OAuth2Authorization oAuth2;
	Authorization auth;
	AccessToken accessToken;
	Properties properties;
	PropertyConfiguration propConf;
	private ArrayList<Tweet> list = new ArrayList<Tweet>();

	public Collect(String topic) throws TwitterException {
		boolean getacc = false;

		properties = new Properties();
		file = new File("twitter.properties");
		if (getProperties() < ALL_KEYS)
			getacc = true;
		propConf = new PropertyConfiguration(properties);
		if (getacc)
			getAccess();
		// twitFactory = new TwitterStreamFactory(propConf);
		TwitterFactory factory = new TwitterFactory();
		Twitter twitter = factory.getInstance();
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
		int count = 0;

		try {
			Query query = new Query(topic);
			QueryResult result;
			do {
				result = twitter.search(query);
				List<Status> statuses = result.getTweets();
				for (Status status : statuses) {
					// System.out.println("@" +
					// tweet.getUser().getScreenName()
					// + " - " + tweet.getText());
					Tweet tweet = Clean(status.getUser().getScreenName(), status.getText());
					list.add(tweet);
					count++;
					// System.out.println("Tweet #" + count + " about " +
					// topic);
				}
			} while (count < 10);
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to search tweets: " + te.getMessage());
			System.exit(-1);
		}

	}

	int getProperties() {

		int read = 0;
		@SuppressWarnings("unused")
		String value = "";
		try {
			InputStream in = this.getClass().getClassLoader().getResourceAsStream("twitter.properties");
			properties.load(in);
			if ((apiKey = properties.getProperty(apiKeyName)) != null)
				read += API_KEY;
			if ((consumerKey = properties.getProperty(consumerKeyName)) != null)
				read += CONSUMER_KEY;
			if ((consumerSecret = properties.getProperty(consumerSecretName)) != null)
				read += CONSUMER_SECRET;
			if ((token = properties.getProperty(aToken)) != null)
				read += TOKEN;
			if ((tokenSecret = properties.getProperty(aTokenSecret)) != null)
				read += TOKEN_TYPE;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (Exception ignore) {
			}
		}
		return read;
	}

	public void getAccess() {
		try {

			twitter = new TwitterFactory(propConf).getInstance();

			// twitter.setOAuthConsumer(consumerKey, consumerSecret);
			System.out.println("customerKey: " + consumerKey);
			System.out.println("customerSecret: " + consumerSecret);
			System.out.println(twitter.getAuthorization().toString());
			requestToken = twitter.getOAuthRequestToken();
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (null == accessToken) {
				System.out.println("Open the following URL and grant access to your account:");
				System.out.println(requestToken.getAuthorizationURL());
				try {
					Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
				String pin = br.readLine();
				try {
					if (pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					} else {
						accessToken = twitter.getOAuthAccessToken(requestToken);
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}

			System.out.println("Got access token.");
			System.out.println("Access token: " + accessToken.getToken());
			System.out.println("Access token secret: " + accessToken.getTokenSecret());

			try {
				properties.setProperty("oauth.accessToken", accessToken.getToken());
				token = accessToken.getToken();
				properties.setProperty("oauth.accessTokenSecret", accessToken.getTokenSecret());
				tokenSecret = accessToken.getTokenSecret();
				outStream = new FileOutputStream(file);
				properties.store(outStream, "twitter4j.properties");
				outStream.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(-1);
			} finally {
				if (outStream != null) {
					try {
						outStream.close();
					} catch (IOException ignore) {
					}
				}
			}
			System.out.println("Successfully stored access token to " + file.getAbsolutePath() + ".");
			System.exit(0);
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to get accessToken: " + te.getMessage());
			System.exit(-1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Failed to read the system input.");
			System.exit(-1);
		}

		// oAuth2 = new OAuth2Authorization(propConf);
		// try{
		// oAuth2Token = oAuth2.getOAuth2Token();
		// }catch(Exception e)
		// {
		// e.printStackTrace();
		// }
	}

	public void writeProperties() {
		try {
			properties.setProperty(apiK, apiKey);
			properties.setProperty(consumer, consumerKey);
			properties.setProperty(consumerSec, consumerSecret);
			properties.setProperty(aToken, oAuth2Token.getAccessToken());
			properties.setProperty(aTokenSecret, oAuth2Token.getTokenType());
			fr = new FileOutputStream(file);
			properties.store(fr, "twitter.properties");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (Exception ignore) {
			}

		}

	}

	// Method: Clean
	// Parameters: string
	// returns: nothing
	// This method will be called to clean a tweet, and then
	// add that cleaned string to the arraylist cleanList.
	public static Tweet Clean(String username, String text) {

		String url = "(https?|http?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		String enter = "(\\r|\\n)";
		String randomSymbols = "[@!&*$<>;?']"; // Leaving # out of this because
												// it precedes key words
		// This should block any of the profanity
		// sorry for spelling them out here
		String beep = "[^!@#$%^&*]*(ass|shit|butthole|cock|penis|pennis|fucker|fuck|crap|cunt|bitch|whore|bastard|vagina)[^!@#$%^&*]*";

		// example string original
		// String original = "Jo!hn Doe: @#marvel is lam*e!
		// https://www.google.com/search?q=hello&oq=hello&aqs=chrome.0.69i59j69i60j69i57j69i60j69i65j69i61.818j0j7&sourceid=chrome&es_sm=93&ie=UTF-8
		// search shows proof.";

		text = text.replaceAll(url, "");
		text = text.replaceAll(enter, "");
		text = text.replaceAll(randomSymbols, "");
		text = text.replaceAll(beep,"-beep-");
		Tweet tweet = new Tweet(username, text);
		return tweet;

	}

	public ArrayList<Tweet> getList() {
		return list;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			Collect col = new Collect(args[i]);
			Cluster cluster = new Cluster(col.getList());
		}

		// col.getAccess();
		// "https://twitter.com/search"
		// "https://api.twitter.com/1.1/search/tweets.json"
		// + "?q=marvel"); //https://stream.twitter.com/1.1/statuses/filter.json

		// if(col.getProperties())
		// {
		// col.connect();
		// System.out.println("open stream, token: " + col.token);
		// String inputLine =
		// }
		// while ((inputLine = in.readLine()) != null)
		// System.out.println(inputLine);
		// in.close();
	}

}