package searchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.service.DefaultService;

@Controller
public class DefaultController {

    private final DefaultService defaultService;

    @Autowired
    public DefaultController(DefaultService defaultService) {
        this.defaultService = defaultService;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("sitePaths", defaultService.getSitePaths());
        return "index";
    }
}
