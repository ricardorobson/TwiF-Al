package rcrdrobson.twifal;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import sun.security.provider.certpath.Vertex;
import twitter4j.*;
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

    private DirectedSubgraph<String, DefaultEdge> getStronglyGraph(Map<Long,Set<Long>> hashMap){
        DirectedGraph<String, DefaultEdge> g
                = new DefaultDirectedGraph<>(DefaultEdge.class);
        for(Long id : hashMap.keySet()){
            Set<Long> statusSet = hashMap.get(id);
            if(statusSet.size()>1){
                Long[] statusArray = statusSet.toArray(new Long[0]);
                for (int i=0;i<statusArray.length;i++){
                    for(int j=i+1;j<statusArray.length;j++){
                        g.addVertex(statusArray[i]+"");
                        g.addVertex(statusArray[j]+"");

                        g.addEdge(statusArray[i]+"",statusArray[j]+"");
                        g.addEdge(statusArray[j]+"",statusArray[i]+"");
                    }
                }
            }
        }
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg = new KosarajuStrongConnectivityInspector<>(g);
        List<DirectedSubgraph<String, DefaultEdge>> listAux = scAlg.stronglyConnectedSubgraphs();
        listAux.sort(Comparator.comparingInt(a -> a.vertexSet().size()));
        return listAux.get(0);
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

    @RequestMapping(value = "/connect", method = RequestMethod.GET)
    public String connect(HttpServletRequest request,
                        @RequestParam("oauth_token") String oauthtoken, @RequestParam("oauth_verifier") String oauthVerifier){
        Twitter twitter = null;
        try {
            twitter = getInstance(request,oauthVerifier);
            Map<Long,Set<Long>> hashMap=getMap(twitter);
            DirectedSubgraph<String, DefaultEdge> graph = getStronglyGraph(hashMap);
            PageRank pageRank = new PageRank(graph);

            System.out.println(pageRank.getScores().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
