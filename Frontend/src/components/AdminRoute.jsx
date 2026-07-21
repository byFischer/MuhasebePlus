import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// Bu kontrol yalnızca kullanıcı deneyimi içindir; gerçek yetkilendirme
// backend'de /api/admin/** üzerindeki ADMIN korumasıyla yapılır.
function AdminRoute({ children }) {
    const { isAuthenticated, user } = useAuth();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    // Profil henüz yükleniyorsa boş iskelet göster (yanlış yönlendirme yapma)
    if (!user) {
        return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
    }

    if (user.role !== 'ADMIN') {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

export default AdminRoute;
