package searchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.service.SiteService;

@Controller
public class DefaultController {

    private final SiteService siteService;

    @Autowired
    public DefaultController(SiteService siteService) {
        this.siteService = siteService;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("sitePaths", siteService.getSitePaths());
        return "index";
    }
}
