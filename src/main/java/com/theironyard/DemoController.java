package com.theironyard;

import com.google.common.util.concurrent.SettableFuture;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.models.AuthorizationCodeCredentials;
import com.wrapper.spotify.models.RefreshAccessTokenCredentials;
import com.wrapper.spotify.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Controller
public class DemoController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirectUri}")
    private String redirectUri;

    @Value("${spotify.scope}")
    private String scope;

    @Value("${spotify.stateKey}")
    private String stateKey;

    @Autowired
    DemoService demoService;

    @RequestMapping(path = "/")
    public String doSomething() {
        return "home";
    }

    @RequestMapping(path = "/login")
    public String doLogin(HttpSession session) {

        String state = String.valueOf(new Random().nextLong());

        session.setAttribute("state", state);

        // configure the API
        final Api api = Api.builder()
                .clientId(this.clientId)
                .clientSecret(this.clientSecret)
                .redirectURI(this.redirectUri)
                .build();

        session.setAttribute("api", api);

        // configure scope for auth
        List<String> scopes = Arrays.asList(this.scope.split(" "));

        // generate auth url for client
        String authorizeURL = api.createAuthorizeURL(scopes, state);

        // redirect to auth page
        return "redirect:" + authorizeURL;
    }

    @RequestMapping(path = "/callback")
    public String doCallback(String code, String state, HttpSession session) {

        Api api = (Api) session.getAttribute("api");
        /* Make a token request. Asynchronous requests are made with the .getAsync method and synchronous requests
         * are made with the .get method. This holds for all type of requests. */
        try {
            AuthorizationCodeCredentials credentials = api.authorizationCodeGrant(code).build().get();

            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

        } catch (IOException | WebApiException e) {
            e.printStackTrace();
        }

        return "redirect:/test";
    }

    @RequestMapping(path="/test")
    public String test(Model model, HttpSession session){
        Api api = (Api) session.getAttribute("api");

        demoService.refreshToken(api);

        User user = demoService.getUser(api);
        model.addAttribute("user", user);

        return "test";
    }
}
