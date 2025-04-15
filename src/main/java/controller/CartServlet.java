package controller;

import model.Medicine;
import model.Order;
import model.OrderItem;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/cart/*")
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/auth/login");
            return;
        }

        out.println("<html><head><title>Online Medical Shop - Cart</title>");
        out.println("<link rel='stylesheet' href='style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'><h1>Your Cart</h1>");

        List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            out.println("<p>Your cart is empty. <a href='/medicines'>Add some medicines</a>.</p>");
        } else {
            out.println("<div class='cart-grid'>");
            BigDecimal cartTotal = BigDecimal.ZERO;
            for (OrderItem item : cart) {
                BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                cartTotal = cartTotal.add(itemTotal);
                out.println("<div class='cart-item'>");
                out.println("<h3>" + item.getMedicine().getName() + "</h3>");
                out.println("<h3>" + item.getMedicine().getBrand() + "</h3>");
                out.println("<p>Price: $" + item.getUnitPrice() + "</p>");
                out.println("<p>Quantity: " + item.getQuantity() + "</p>");
                out.println("<p>Total: $" + itemTotal + "</p>");
                out.println("</div>");
            }
            out.println("</div>");
            out.println("<p><strong>Cart Total: $" + cartTotal + "</strong></p>");
            out.println("<form method='post' action='/cart/checkout'><button type='submit'>Checkout</button></form>");
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

        out.println("<html><head><title>Online Medical Shop - Cart</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo)) {
                Integer medicineId = Integer.parseInt(req.getParameter("medicineId"));
                Medicine medicine = dbSession.get(Medicine.class, medicineId);
                if (medicine == null || medicine.getStock() <= 0) {
                    out.println("<h1>Error</h1><p>Medicine not available. <a href='/medicines'>Back to Medicines</a>.</p>");
                } else {
                    List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
                    if (cart == null) {
                        cart = new ArrayList<>();
                        session.setAttribute("cart", cart);
                    }
                    OrderItem item = cart.stream()
                        .filter(ci -> ci.getMedicine().getMedicineId().equals(medicineId))
                        .findFirst()
                        .orElse(null);
                    if (item == null) {
                        OrderItem newItem = new OrderItem();
                        newItem.setMedicine(medicine);
                        newItem.setQuantity(1);
                        newItem.setUnitPrice(medicine.getPrice());
                        cart.add(newItem);
                        System.out.println("Added new item to cart: " + medicine.getName() + ", Quantity: 1");
                    } else if (item.getQuantity() < medicine.getStock()) {
                        item.setQuantity(item.getQuantity() + 1);
                        System.out.println("Updated item in cart: " + medicine.getName() + ", Quantity: " + item.getQuantity());
                    } else {
                        out.println("<h1>Error</h1><p>Not enough stock. <a href='/cart'>Back to Cart</a>.</p>");
                        out.println("</div></body></html>");
                        return;
                    }
                    resp.sendRedirect("/cart");
                }
            } else if ("/checkout".equals(pathInfo)) {
                List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
                if (cart == null || cart.isEmpty()) {
                    out.println("<h1>Error</h1><p>Cart is empty. <a href='/medicines'>Add medicines</a>.</p>");
                } else {
                    Order order = new Order();
                    order.setUser(user);
                    order.setOrderItems(cart);
                    order.setTotalAmount(cart.stream()
                        .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                    order.setStatus("Pending");

                    dbSession.beginTransaction();
                    dbSession.persist(order);
                    for (OrderItem item : cart) {
                        item.setOrder(order); 
                        dbSession.persist(item);
                        Medicine medicine = item.getMedicine();
                        medicine.setStock(medicine.getStock() - item.getQuantity());
                        dbSession.merge(medicine);
                    }
                    dbSession.getTransaction().commit();

                    session.removeAttribute("cart");
                    out.println("<h1>Success</h1><p>Order placed! <a href='/orders'>View Orders</a>.</p>");
                }
            } else {
                out.println("<h1>Error</h1><p>Invalid action.</p>");
            }
        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Cart operation failed: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
        out.println("</div></body></html>");
    }

    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");
        nav.append("<li><a href='/auth'>Home</a></li>");
        nav.append("<li><a href='/medicines'>Medicines</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");
        nav.append("<li><a href='/orders'>Orders</a></li>");
        nav.append("<li><a href='/medicines/add'>Add Medicine</a></li>");
        nav.append("<li><a href='/auth/logout'>Logout (" + user.getUsername() + ")</a></li>");
        nav.append("</ul>");
        return nav.toString();
    }
}
