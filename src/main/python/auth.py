import sys
import os
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError
from dotenv import load_dotenv
from datetime import datetime, timedelta

# Get the directory containing the Python script
script_dir = os.path.dirname(os.path.abspath(__file__))

load_dotenv(os.path.join(script_dir, "..", "..", "src", "main", "resources", ".env"))

if not all(os.getenv(var) for var in ['DB_USER', 'DB_PASSWORD', 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_SCHEMA']):
    print("Error: Missing required environment variables", file=sys.stderr)
    print("Required variables: DB_USER, DB_PASSWORD, DB_HOST, DB_PORT, DB_NAME, DB_SCHEMA")
    sys.exit(1)

class DatabaseConnection:
    @staticmethod
    def get_engine():
        """Create and return a database engine using environment variables"""
        print("Attempting to connect to database...")
        try:
            # Get database connection parameters from environment variables
            db_url = f"postgresql://{os.getenv('DB_USER')}:{os.getenv('DB_PASSWORD')}@{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_NAME')}"
            
            # Hide password in logs
            safe_url = db_url.replace(os.getenv('DB_PASSWORD'), '****')
            print(f"Database URL: {safe_url}")
            
            engine = create_engine(db_url, client_encoding='utf8')
            
            # Test connection
            with engine.connect() as conn:
                print("Successfully connected to database")
                
            return engine
            
        except UnicodeDecodeError as e:
            print(f"Encoding error in database connection string: {str(e)}", file=sys.stderr)
            raise
        except Exception as e:
            print(f"Failed to connect to database: {str(e)}", file=sys.stderr)
            raise

class UserAuth:
    @staticmethod
    def create_pending_registration(username, password, email, token):
        """Create a pending registration"""
        try:
            engine = DatabaseConnection.get_engine()
            
            with engine.connect() as conn:
                # Check if username or email already exists in users table
                check_existing_user_sql = """
                    SELECT username, email_address 
                    FROM webserver.users 
                    WHERE username = :username 
                    OR email_address = :email
                """
                existing_user = conn.execute(
                    text(check_existing_user_sql),
                    {'username': username, 'email': email}
                ).fetchone()
                
                if existing_user:
                    if existing_user.username == username:
                        print("ERROR: Username is already registered")
                        return
                    print("ERROR: Email address is already registered")
                    return
                
                # Check pending registrations
                check_pending_sql = """
                    SELECT token 
                    FROM webserver.pending_registrations 
                    WHERE username = :username 
                    OR email_address = :email
                """
                existing_pending = conn.execute(
                    text(check_pending_sql),
                    {'username': username, 'email': email}
                ).fetchone()
                
                if existing_pending:
                    # Update existing pending registration
                    update_pending_sql = """
                        UPDATE webserver.pending_registrations 
                        SET token = :token,
                            password = :password,
                            expiry_time = NOW() + INTERVAL '24 hours'
                        WHERE username = :username 
                        OR email_address = :email
                    """
                    conn.execute(
                        text(update_pending_sql),
                        {'token': token, 'password': password, 'username': username, 'email': email}
                    )
                    conn.commit()
                    print("SUCCESS: Verification email resent")
                    return
                
                # Create new registration
                create_pending_sql = """
                    INSERT INTO webserver.pending_registrations 
                        (token, username, password, email_address, expiry_time) 
                    VALUES 
                        (:token, :username, :password, :email, NOW() + INTERVAL '24 hours')
                """
                conn.execute(
                    text(create_pending_sql),
                    {'token': token, 'username': username, 'password': password, 'email': email}
                )
                conn.commit()
                print("SUCCESS: Verification email sent")
      

        except SQLAlchemyError as error:
            print(f"ERROR: Database error - {str(error)}")


    @staticmethod
    def verify_and_register(username, token):
        """Verify token and create user if valid"""
        try:
            engine = DatabaseConnection.get_engine()
            
            with engine.connect() as conn:
                with conn.begin():
                    # Check pending registration
                    check_pending_sql = """
                        SELECT username, password, email_address 
                        FROM webserver.pending_registrations 
                        WHERE token = :token 
                        AND username = :username 
                        AND expiry_time > NOW()
                    """
                    pending = conn.execute(
                        text(check_pending_sql),
                        {'token': token, 'username': username}
                    ).fetchone()
                    
                    if not pending:
                        print("ERROR: Invalid or expired verification token")
                        return
                    
                    # Check if user already exists
                    check_user_sql = """
                        SELECT username 
                        FROM webserver.users 
                        WHERE username = :username 
                        OR email_address = :email
                    """
                    existing_user = conn.execute(
                        text(check_user_sql),
                        {'username': pending.username, 'email': pending.email_address}
                    ).fetchone()
                    
                    if existing_user:
                        print("ERROR: User already exists")
                        return
                    
                    # Create verified user
                    create_user_sql = """
                        INSERT INTO webserver.users 
                            (username, password, email_address, created_time, updated_time, is_verified) 
                        VALUES 
                            (:username, :password, :email, NOW(), NOW(), true)
                    """
                    conn.execute(
                        text(create_user_sql),
                        {'username': pending.username, 'password': pending.password, 'email': pending.email_address}
                    )
                    
                    # Clean up pending registration
                    delete_pending_sql = """
                        DELETE FROM webserver.pending_registrations 
                        WHERE username = :username 
                        OR email_address = :email
                        OR expiry_time < NOW()
                    """
                    conn.execute(
                        text(delete_pending_sql),
                        {'username': pending.username, 'email': pending.email_address}
                    )
                    
                    print("SUCCESS: Email verified successfully")

        except SQLAlchemyError as error:
            print(f"ERROR: Database error - {str(error)}")

    @staticmethod
    def check_credentials(username, password):
        """Verify user credentials"""
        try:
            engine = DatabaseConnection.get_engine()
            check_credentials_sql = """
                SELECT username 
                FROM webserver.users 
                WHERE username = :username 
                AND password = :password 
                AND is_verified = true
            """
            # Print the SQL with actual values (masking password)
            debug_sql = check_credentials_sql.replace(":username", username).replace(":password", password)
            print("Executing SQL:", debug_sql)
            
            with engine.connect() as conn:
                result = conn.execute(
                    text(check_credentials_sql),
                    {'username': username, 'password': password}
                ).fetchall()
                
                if len(result) > 0:
                    print("SUCCESS: Login successful")
                else:
                    print("ERROR: Invalid username or password")
            return
        except SQLAlchemyError as error:
            print(f"ERROR: Database error - {str(error)}")


def main():
    """Main entry point for command line interface"""
    if len(sys.argv) < 3:
        print("ERROR: Insufficient arguments")
        return

    command = sys.argv[1]
    username = sys.argv[2]
    
    try:
        if command == "check":
            if len(sys.argv) < 4:
                print("ERROR: Password required")
                return
            password = sys.argv[3]
            UserAuth.check_credentials(username, password)

        
        elif command == "pending_register":
            if len(sys.argv) < 6:
                print("ERROR: Email and token required")
                return
            password = sys.argv[3]
            email = sys.argv[4]
            token = sys.argv[5]
            UserAuth.create_pending_registration(username, password, email, token)
 
        
        elif command == "verify_register":
            if len(sys.argv) < 4:
                print("ERROR: Token required")
                return
            token = sys.argv[3]
            UserAuth.verify_and_register(username, token)

        
        else:
            print(f"ERROR: Unknown command {command}")
            return

    except Exception as e:
        print(f"ERROR: {str(e)}")
        return

if __name__ == "__main__":
    main() 