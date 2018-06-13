package rcrdrobson.twifal;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.net.URI;

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
    public ResponseEntity<Object> login(){
        Twitter twitter = getInstance();
        RequestToken requestToken = null;
        try {
            requestToken = twitter.getOAuthRequestToken();
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(requestToken.getAuthorizationURL())).build();
        } catch (TwitterException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(){
        return "OK";
    }
}
