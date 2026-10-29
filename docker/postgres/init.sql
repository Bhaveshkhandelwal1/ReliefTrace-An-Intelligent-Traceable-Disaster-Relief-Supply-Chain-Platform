-- DisasterReliefNet Database Initialization Script
-- Creates dedicated databases for each microservice and Keycloak (database-per-service pattern)

SELECT 'CREATE DATABASE keycloak_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak_db')\gexec

SELECT 'CREATE DATABASE disaster_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'disaster_db')\gexec

SELECT 'CREATE DATABASE donor_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'donor_db')\gexec

SELECT 'CREATE DATABASE receiver_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'receiver_db')\gexec

SELECT 'CREATE DATABASE verification_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'verification_db')\gexec

SELECT 'CREATE DATABASE finance_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'finance_db')\gexec

SELECT 'CREATE DATABASE inventory_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec

SELECT 'CREATE DATABASE matching_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'matching_db')\gexec

SELECT 'CREATE DATABASE transport_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'transport_db')\gexec

SELECT 'CREATE DATABASE notification_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

SELECT 'CREATE DATABASE analytics_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analytics_db')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE disaster_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE donor_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE receiver_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE verification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE finance_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE matching_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE transport_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
