package com.patroltracker.controller;

import com.patroltracker.model.User;
import com.patroltracker.service.PatrolService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class PatrolWebController {

    @Autowired
    private PatrolService patrolService;

    private String getAuthenticatedUserId(HttpSession session, String cookieUserId, String paramUserId) {
        if (paramUserId != null && !paramUserId.isBlank()) {
            return paramUserId;
        }
        if (session.getAttribute("loggedInUserId") != null) {
            return (String) session.getAttribute("loggedInUserId");
        }
        if (cookieUserId != null && !cookieUserId.isBlank() && !"null".equalsIgnoreCase(cookieUserId)) {
            return cookieUserId;
        }
        return null; // Unauthenticated
    }

    private void populateCommonModel(Model model, String activeUserId) {
        List<User> allUsers = patrolService.getAllUsers();
        User activeUser = patrolService.getUserById(activeUserId)
                .orElseGet(() -> allUsers.isEmpty() ? new User("Patrol Tracker", "Patrol Duty Monitor", "Patrol Duty Monitor", "ADM-00", "BXRadmin123", "+91-9990001112", "Active") : allUsers.get(0));

        boolean isAdmin = "Patrol Duty Monitor".equalsIgnoreCase(activeUser.getRole()) || "Admin".equalsIgnoreCase(activeUser.getRole());
        boolean isStationInCharge = "Supervisor".equalsIgnoreCase(activeUser.getRole()) || isAdmin;

        model.addAttribute("activeUserId", activeUser.getUserId());
        model.addAttribute("activeUser", activeUser);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isStationInCharge", isStationInCharge);
        model.addAttribute("allUsers", allUsers);
    }

    // Login GET page
    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid Username or Password! Please try again.");
        }
        return "login";
    }

    // Login POST handler
    @PostMapping("/login")
    public String processLogin(@RequestParam("userId") String userId,
                               @RequestParam("password") String password,
                               HttpSession session,
                               HttpServletResponse response,
                               Model model) {
        Optional<User> authenticatedUser = patrolService.authenticate(userId, password);
        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInUserRole", user.getRole());

            Cookie cookie = new Cookie("patrol_user", user.getUserId());
            cookie.setPath("/");
            cookie.setMaxAge(864000);
            response.addCookie(cookie);

            return "redirect:/scan-logs?activeUser=" + user.getUserId();
        } else {
            return "redirect:/login?error=1";
        }
    }

    // Logout handler
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        Cookie cookie = new Cookie("patrol_user", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login";
    }

    @GetMapping("/")
    public String index(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                        @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                        HttpSession session,
                        Model model) {
        return scanLogs(activeUserParam, activeUserCookie, session, model);
    }

    @GetMapping("/scan-logs")
    public String scanLogs(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                           @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                           HttpSession session,
                           Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "scan-logs");
        model.addAttribute("scanLogs", patrolService.getScanLogsForUser(activeUserId));
        model.addAttribute("checkpoints", patrolService.getAllCheckpoints());
        model.addAttribute("users", patrolService.getUsersForUser(activeUserId));
        model.addAttribute("duties", patrolService.getDutiesForUser(activeUserId));
        model.addAttribute("analytics", patrolService.getPatrolAnalyticsForUser(activeUserId));

        return "scan-logs";
    }

    @GetMapping("/duty")
    public String dutyAllocation(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                                 @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                                 HttpSession session,
                                 Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "duty");
        model.addAttribute("duties", patrolService.getDutiesForUser(activeUserId));
        model.addAttribute("users", patrolService.getAllUsers());
        model.addAttribute("checkpoints", patrolService.getAllCheckpoints());
        model.addAttribute("analytics", patrolService.getPatrolAnalyticsForUser(activeUserId));

        return "duty";
    }

    @GetMapping("/map")
    public String mapLocation(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                              @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                              HttpSession session,
                              Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "map");
        model.addAttribute("checkpoints", patrolService.getAllCheckpoints());
        model.addAttribute("scanLogs", patrolService.getScanLogsForUser(activeUserId));
        model.addAttribute("users", patrolService.getUsersForUser(activeUserId));

        return "map";
    }

    @GetMapping("/users")
    public String userList(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                           @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                           HttpSession session,
                           Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "user");
        model.addAttribute("users", patrolService.getUsersForUser(activeUserId));

        return "users";
    }

    @GetMapping("/checkpoints")
    public String checkpointsList(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                                  @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                                  HttpSession session,
                                  Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "checkpoints");
        model.addAttribute("checkpoints", patrolService.getAllCheckpoints());

        return "checkpoints";
    }

    @GetMapping("/archive")
    public String archiveLogs(@RequestParam(name = "activeUser", required = false) String activeUserParam,
                              @CookieValue(name = "patrol_user", required = false) String activeUserCookie,
                              HttpSession session,
                              Model model) {
        String activeUserId = getAuthenticatedUserId(session, activeUserCookie, activeUserParam);
        if (activeUserId == null) {
            return "redirect:/login";
        }
        populateCommonModel(model, activeUserId);

        model.addAttribute("activeTab", "archive");
        model.addAttribute("archives", patrolService.getArchivesForUser(activeUserId));
        model.addAttribute("scanLogs", patrolService.getScanLogsForUser(activeUserId));
        model.addAttribute("analytics", patrolService.getPatrolAnalyticsForUser(activeUserId));

        return "archive";
    }
}
