import Sidebar from '../../components/admin/Sidebar';
import Footer from '../../components/admin/Footer';
import Header from '../../components/admin/Header';
import { Outlet } from 'react-router-dom';
import { useAuth } from '../../auth/admin/useAuth';
import Spinner from '../../components/Spinner';

const AdminLayout = () => {
 const { user, account, loading } = useAuth();

  if (loading || !user || !account) return <Spinner />;
   
  
  return (
    <div>
      <Header />
      
        <Sidebar />
        
          <Outlet />
    
      
      <Footer />
    </div>
  );
};

export default AdminLayout;
