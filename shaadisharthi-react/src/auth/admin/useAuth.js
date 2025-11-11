import { useContext } from "react";
import { AuthContext } from "../../auth/admin/AuthContext";

//export const useAuth = () => useContext(AuthContext);
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}