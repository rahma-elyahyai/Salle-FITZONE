package ginf.demosalle.filter;

import ginf.demosalle.model.Utilisateur;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final String[] PUBLIQUES = {
            "/",
            "/index.jsp",
            "/landingpage.xhtml",
            "/login.xhtml",
            "/register.xhtml",
            "/acces-refuse.xhtml",
            "/jakarta.faces.resource",
            "/resources/"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ctx = request.getContextPath();
        String path = request.getRequestURI().substring(ctx.length());

        for (String pub : PUBLIQUES) {
            if (path.equals(pub) || path.startsWith(pub)
                    || path.endsWith(".css")
                    || path.endsWith(".js")
                    || path.endsWith(".png")
                    || path.endsWith(".ico")
                    || path.endsWith(".jpg")
                    || path.endsWith(".jpeg")
                    || path.endsWith(".webp")) {
                chain.doFilter(req, res);
                return;
            }
        }

        HttpSession session = request.getSession(false);
        Utilisateur user = session != null
                ? (Utilisateur) session.getAttribute("utilisateur")
                : null;

        if (user == null) {
            response.sendRedirect(ctx + "/login.xhtml");
            return;
        }

        String role = user.getRole().name();

        if (path.startsWith("/coach/") && !"COACH".equals(role)) {
            response.sendRedirect(ctx + "/acces-refuse.xhtml");
            return;
        }
        if (path.startsWith("/membre/") && !"MEMBRE".equals(role)) {
            response.sendRedirect(ctx + "/acces-refuse.xhtml");
            return;
        }
        if (path.startsWith("/admin/") && !"ADMIN".equals(role)) {
            response.sendRedirect(ctx + "/acces-refuse.xhtml");
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig fc) {}

    @Override
    public void destroy() {}
}