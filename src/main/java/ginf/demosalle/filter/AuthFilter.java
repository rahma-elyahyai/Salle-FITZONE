package ginf.demosalle.filter;

import ginf.demosalle.model.Role;
import ginf.demosalle.model.Utilisateur;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        // Ressources publiques — laisser passer
        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession  session = request.getSession(false);
        Utilisateur  u       = (session != null)
                ? (Utilisateur) session.getAttribute("utilisateurConnecte")
                : null;

        // Non connecté → login
        if (u == null) {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }

        // Mauvais rôle → redirection vers son dashboard
        if (path.startsWith("/coach/") && u.getRole() != Role.COACH) {
            response.sendRedirect(request.getContextPath() + "/membre/dashboard.xhtml");
            return;
        }
        if (path.startsWith("/membre/") && u.getRole() != Role.MEMBRE) {
            response.sendRedirect(request.getContextPath() + "/coach/dashboard.xhtml");
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        return path.equals("/login.xhtml")
                || path.equals("/register.xhtml")
                || path.equals("/")
                || path.equals("/index.jsp")
                || path.equals("/landingpage.xhtml")
                || path.equals("/acces-refuse.xhtml")
                || path.startsWith("/jakarta.faces.resource/")
                || path.startsWith("/resources/")
                || path.contains(".css")
                || path.contains(".js")
                || path.contains(".png")
                || path.contains(".jpg")
                || path.contains(".ico");
    }
}