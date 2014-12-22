package au.com.jakebarnes.JourneyPlanner;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class GUI
{
  @RequestMapping(method = RequestMethod.GET)
  public String index(ModelMap model)
  {
    return "index";
  }
}
