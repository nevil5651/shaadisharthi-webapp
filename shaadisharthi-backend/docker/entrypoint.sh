#!/bin/sh
set -e

CONFIG_FILE="/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/config.properties"
mkdir -p "$(dirname "$CONFIG_FILE")"

# Wait for MySQL to be reachable (if DB_URL contains mysql host)
wait_for_mysql() {
  # parse host and port from DB_URL (jdbc:mysql://host:port/db)
  if [ -z "${DB_URL:-}" ]; then
    return 0
  fi
  # extract host:port using parameter expansion
  hostport=$(echo "$DB_URL" | sed -E 's#jdbc:mysql://([^/]+)/.*#\1#')
  host=$(echo "$hostport" | cut -d: -f1)
  port=$(echo "$hostport" | cut -s -d: -f2)
  if [ -z "$host" ]; then
    return 0
  fi
  port=${port:-3306}
  echo "Waiting for MySQL at $host:$port ..."
  n=0
  until nc -z "$host" "$port" >/dev/null 2>&1 || [ $n -ge 30 ]; do
    n=$((n+1))
    echo "  -> waiting ($n)..."
    sleep 2
  done
  if [ $n -ge 30 ]; then
    echo "Warning: MySQL did not respond after waiting. Continuing and Tomcat may fail until DB is ready."
  else
    echo "MySQL reachable."
  fi
}

# generate config.properties
cat > "$CONFIG_FILE" <<EOF
# generated at container start
jwt.secret.key=${JWT_SECRET_KEY:-}
jwt.reset.secret.key=${JWT_RESET_SECRET_KEY:-}

# Database
db.url=${DB_URL:-jdbc:mysql://mysql:3306/shaadisharthi}
db.username=${DB_USERNAME:-root}
db.password=${DB_PASSWORD:-}
db.driver=${DB_DRIVER:-com.mysql.cj.jdbc.Driver}

# Base App Url
APP_BASE_URL=${APP_BASE_URL}

# Email
email.from=${EMAIL_FROM:-}
email.password=${EMAIL_PASSWORD:-}

# Cloudinary
cloudinary.cloud_name=${CLOUDINARY_CLOUD_NAME:-}
cloudinary.api_key=${CLOUDINARY_API_KEY:-}
cloudinary.api_secret=${CLOUDINARY_API_SECRET:-}

# CORS
admin.allowed.origins=${ADMIN_ALLOWED_ORIGINS:-}
serviceprovider.allowed.origins=${SERVICEPROVIDER_ALLOWED_ORIGINS:-}
customer.allowed.origins=${CUSTOMER_ALLOWED_ORIGINS:-}

# Roles (many lines)
account.get.roles=${ACCOUNT_GET_ROLES:-}
account.post.roles=${ACCOUNT_POST_ROLES:-}
dashboardstats.get.roles=${DASHBOARDSTATS_GET_ROLES:-}
changepassword.post.roles=${CHANGEPASSWORD_POST_ROLES:-}
customers.get.roles=${CUSTOMERS_GET_ROLES:-}
customers.post.roles=${CUSTOMERS_POST_ROLES:-}
service-providers.get.roles=${SERVICE_PROVIDERS_GET_ROLES:-}
service-providers.post.roles=${SERVICE_PROVIDERS_POST_ROLES:-}
queries.get.roles=${QUERIES_GET_ROLES:-}
queries.post.roles=${QUERIES_POST_ROLES:-}
guestqueryhandler.get.roles=${GUESTQUERYHANDLER_GET_ROLES:-}
guestqueryhandler.post.roles=${GUESTQUERYHANDLER_POST_ROLES:-}
adminlogout.post.roles=${ADMINLOGOUT_POST_ROLES:-}
update-status.post.roles=${UPDATE_STATUS_POST_ROLES:-}
usertrends.get.roles=${USERTRENDS_GET_ROLES:-}
ordercomparison.get.roles=${ORDERCOMPARISON_GET_ROLES:-}
providerdashboardstats.get.roles=${PROVIDERDASHBOARDSTATS_GET_ROLES:-}
logout.post.roles=${LOGOUT_POST_ROLES:-}
provider-change-password.post.roles=${PROVIDER_CHANGE_PASSWORD_POST_ROLES:-}
providerservices.get.roles=${PROVIDERSERVICES_GET_ROLES:-}
cloudinarysignature.post.roles=${CLOUDINARYSIGNATURE_POST_ROLES:-}
createservice.post.roles=${CREATESERVICE_POST_ROLES:-}
editservice.put.roles=${EDITSERVICE_PUT_ROLES:-}
editservice.post.roles=${EDITSERVICE_POST_ROLES:-}
editservice.delete.roles=${EDITSERVICE_DELETE_ROLES:-}
deleteservice.delete.roles=${DELETESERVICE_DELETE_ROLES:-}
business-register.post.roles=${BUSINESS_REGISTER_POST_ROLES:-}
AddSupportQuery.post.roles=${ADDSUPPORTQUERY_POST_ROLES:-}
booking-list-servlet.get.roles=${BOOKING_LIST_SERVLET_GET_ROLES:-}
booking-action.post.roles=${BOOKING_ACTION_POST_ROLES:-}
services.get.roles=${SERVICES_GET_ROLES:-}
reviews.post.roles=${REVIEWS_POST_ROLES:-}
cstmr-acc.get.roles=${CSTMR_ACC_GET_ROLES:-}
cstmr-acc.post.roles=${CSTMR_ACC_POST_ROLES:-}
service-detail.get.roles=${SERVICE_DETAIL_GET_ROLES:-}
process-bookings.post.roles=${PROCESS_BOOKINGS_POST_ROLES:-}
cstmr-bookings.get.roles=${CSTMR_BOOKINGS_GET_ROLES:-}
cstmr-action.post.roles=${CSTMR_ACTION_POST_ROLES:-}
cstmr-upcoming-bookings.get.roles=${CSTMR_UPCOMING_BOOKINGS_GET_ROLES:-}
EOF

echo "Wrote config.properties to $CONFIG_FILE"
cat "$CONFIG_FILE"

# Wait for DB optionally
wait_for_mysql

# start Tomcat in foreground
exec catalina.sh run

