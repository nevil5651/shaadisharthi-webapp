export type UserStatus = 'BASIC_REGISTERED' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';

/**
 * Represents the user profile data from the /ServiceProvider/account endpoint,
 * based on your `account.ts` form.
 */
export interface UserProfile {
  name: string;
  email: string;
  state: string;
  phone: string;
  alternate_phone?: string;
  business_name: string;
  address: string;
}

/**
 * Represents the raw response from the /ServiceProvider/login endpoint.
 */
export interface LoginApiResponse {
  token: string;
  provider_id: string; // Note the underscore from the API
  status: UserStatus;
  role: string;
}

/**
 * Represents the complete user object stored in the AuthService.
 * It's a combination of data from login and profile endpoints.
 */
export type User = Omit<LoginApiResponse, 'provider_id'> & { providerId: string } & Partial<UserProfile>;

/**
 * Represents the data structure for the change password form.
 */
export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  renewPassword: string;
}
