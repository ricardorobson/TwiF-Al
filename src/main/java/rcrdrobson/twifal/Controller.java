package rcrdrobson.twifal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
public class Controller {

    private Twitter getInstance(){
        return new TwitterFactory().getInstance();
    }

    private Twitter getInstance(String token, String tokenSecret){
        return new TwitterFactory().getInstance(new AccessToken(token,tokenSecret));
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ResponseEntity<Object> login(HttpServletRequest request){
        Twitter twitter = getInstance();
        RequestToken requestToken = null;
        try {
            requestToken = twitter.getOAuthRequestToken();
            request.getSession().setAttribute("REQUEST_TOKEN", requestToken);
            request.getSession().setAttribute("twitter", twitter);
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(requestToken.getAuthorizationURL())).build();
        } catch (TwitterException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request,
                        @RequestParam("oauth_token") String oauthtoken, @RequestParam("oauth_verifier") String oauthVerifier){

        Twitter twitter = (Twitter) request.getSession().getAttribute("twitter");
        RequestToken requestToken = (RequestToken) request.getSession().getAttribute("REQUEST_TOKEN");
        AccessToken token = (AccessToken) request.getSession().getAttribute("AccessToken");
        System.out.println(requestToken);
        try {
            if(token == null){
                token = twitter.getOAuthAccessToken(requestToken, oauthVerifier);
                request.getSession().setAttribute("AccessToken", token);
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        if(token == null) {
            return "Acesso negado";
        }
        twitter = getInstance(token.getToken(), token.getTokenSecret());
        System.out.println(twitter.getAuthorization().isEnabled());
        StringBuilder result = new StringBuilder();
        try {
            List<Status> statuses = twitter.getHomeTimeline();
            System.out.println("Showing home timeline.");
            for (Status status : statuses) {
                result.append(status.getUser().getName()).append(":").append(status.getText()).append("\n");
            }
        } catch (Exception e){
            e.printStackTrace();
            return "Timeline inacessível";
        }
        System.out.println("finish");
        return result.toString();
    }
}
