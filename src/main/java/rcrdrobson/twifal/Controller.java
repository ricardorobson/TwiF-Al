package rcrdrobson.twifal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import twitter4j.*;
import twitter4j.api.FriendsFollowersResources;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
public class Controller {

    private Twitter getInstance(){
        return new TwitterFactory().getInstance();
    }

    private Twitter getInstance(String token, String tokenSecret){
        return new TwitterFactory().getInstance(new AccessToken(token,tokenSecret));
    }

    private Twitter getInstance(HttpServletRequest request, String oauthVerifier) throws Exception {
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
            throw new Exception("Acesso negado");
        }
        twitter = getInstance(token.getToken(), token.getTokenSecret());
        return twitter;
    }

    private Map<Long, Set<Long>> getMap(Twitter twitter){
        Map<Long, Set<Long>> hashMap = new HashMap<>();
        try {
            //TODO: Get all ids in all pagea
            long[] ids = twitter.getFriendsIDs(-1).getIDs();

            for(long id : ids){
                Set<Long> set=new HashSet<Long>();
                hashMap.put(id,set);
                //Getting favorites tweets from user
                for(Status status : twitter.getFavorites(id)){
                    set.add(status.getId());
                }
                hashMap.put(id,set);

                /*
                //TODO:
                //Getting retweets from tweet from user
                Set<Long> setAux=hashMap.get(id);
                for(Long statusId : setAux){
                    Set<Long> setRetweetAux = new HashSet<Long>();
                    for(Status retweet : twitter.getRetweets(statusId)){
                        setRetweetAux.add(retweet.getId());
                    }
                    if(hashMap.get(id)==null){
                        hashMap.put(id,setAux);
                    }else{
                        setAux.addAll(setRetweetAux);
                        hashMap.put(id,setAux);
                    }
                }
                */
            }

        } catch (TwitterException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hashMap;
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
        try {
            Twitter twitter = getInstance(request,oauthVerifier);
            getTuples(twitter);
            //TODO: Get all ids in all pagea
            long[] ids = twitter.getFriendsIDs(-1).getIDs();
            for(long id : ids){
                System.out.println("ID:\t"+id);
                System.out.println("\t\t"+twitter.getUserTimeline(id).get(0));
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
