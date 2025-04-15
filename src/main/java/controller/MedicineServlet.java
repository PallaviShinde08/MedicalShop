package controller;

import model.Medicine;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/medicines/*")
public class MedicineServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        
        out.println("<html><head><title>Online Medical Shop - Medicines</title>");
        out.println("<link rel='stylesheet' href='style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");
        
        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo) && user != null) {
                out.println("<h1>Add New Medicine</h1>");
                out.println("<form method='post' action='/medicines/add'>");
                out.println("<input type='text' name='name' placeholder='Name' required>");
                out.println("<input type='text' name='brand' placeholder='Brand' required>");
                out.println("<input type='number' name='price' placeholder='Price' step='0.01' required>");
                out.println("<input type='number' name='stock' placeholder='Stock' required>");
                out.println("<input type='date' name='expiryDate' placeholder='Expiry Date' required>");
                out.println("<button type='submit'>Add Medicine</button>");
                out.println("</form>");
            } else {
                out.println("<h1>Our Medicines</h1>");
                // Fixed query here (no 'category' association in the model)
                List<Medicine> medicines = dbSession.createQuery(
                    "FROM Medicine m", Medicine.class
                ).list();
                out.println("<div class='medicine-grid'>");
                for (Medicine medicine : medicines) {
                    out.println("<div class='medicine-card'>");
                    out.println("<h3>" + medicine.getName() + "</h3>");
                    out.println("<p>Brand: " + medicine.getBrand() + "</p>");
                    out.println("<p class='price'>$" + medicine.getPrice() + "</p>");
                    out.println("<p>Stock: " + medicine.getStock() + "</p>");
                    // Optionally, display the expiry date
                    // out.println("<p>Expiry Date: " + new SimpleDateFormat("dd-MM-yyyy").format(medicine.getExpiryDate()) + "</p>");
                    if (user != null) {
                        out.println("<form method='post' action='/cart/add'>");
                        out.println("<input type='hidden' name='medicineId' value='" + medicine.getMedicineId() + "'>");
                        out.println("<button type='submit'>Add to Cart</button>");
                        out.println("</form>");
                    }
                    out.println("</div>");
                }
                out.println("</div>");
            }
        }
        out.println("</div></body></html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String pathInfo = req.getPathInfo();

        if (user == null) {
            resp.sendRedirect("/auth/login");
            return;
        }

        out.println("<html><head><title>Online Medical Shop - Medicines</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");
        
        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo)) {
                Medicine medicine = new Medicine();
                medicine.setName(req.getParameter("name"));
                medicine.setBrand(req.getParameter("brand"));
                medicine.setPrice(new BigDecimal(req.getParameter("price")));
                medicine.setStock(Integer.parseInt(req.getParameter("stock")));
                // Parse expiry date using the format "yyyy-MM-dd"
                //medicine.setExpiryDate(LocalDate.parse(req.getParameter("expiryDate")));
                
                dbSession.beginTransaction();
                dbSession.persist(medicine);
                dbSession.getTransaction().commit();
                
                out.println("<h1>Success</h1><p>Medicine added! <a href='/medicines'>Back to Medicines</a>.</p>");
            }
        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Failed to add medicine: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
        out.println("</div></body></html>");
    }
    
    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");
        nav.append("<li><a href='/auth'>Home</a></li>");
        nav.append("<li><a href='/medicines'>Medicines</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");
        if (user != null) {
            nav.append("<li><a href='/orders'>Orders</a></li>");
            nav.append("<li><a href='/medicines/add'>Add Medicine</a></li>");
            nav.append("<li><a href='/auth/logout'>Logout (" + user.getUsername() + ")</a></li>");
        } else {
            nav.append("<li><a href='/auth/login'>Login</a></li>");
            nav.append("<li><a href='/auth/register'>Register</a></li>");
        }
        nav.append("</ul>");
        return nav.toString();
    }
}
