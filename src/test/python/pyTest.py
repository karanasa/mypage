import sys
import os
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError
from dotenv import load_dotenv

# Get the directory containing the Python script
script_dir = os.path.dirname(os.path.abspath(__file__))

# Load .env file from the same directory as the script
load_dotenv(os.path.join(script_dir, ".env"))

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
    def check_credentials(username, password):
        """Verify user credentials"""
        try:
            engine = DatabaseConnection.get_engine()
            query = text('SELECT username FROM webserver.users WHERE username = :username AND password = :password')
            
            with engine.connect() as conn:
                result = conn.execute(
                    query,
                    {'username': username, 'password': password}
                ).fetchall()
                
                return len(result) > 0

        except SQLAlchemyError as error:
            print(f"Database error: {error}", file=sys.stderr)
            return False
        finally:
            if 'engine' in locals():
                engine.dispose()

    @staticmethod
    def register_user(username, password, email):
        """Register a new user"""
        try:
            engine = DatabaseConnection.get_engine()
            
            # Check if username or email already exists
            check_query = text('SELECT username FROM webserver.users WHERE username = :username OR email_address = :email')
            with engine.connect() as conn:
                existing_user = conn.execute(
                    check_query,
                    {'username': username, 'email': email}
                ).fetchall()
                
                if existing_user:
                    return False  # Username or email already exists

                # Insert new user with current timestamp
                insert_query = text('''
                    INSERT INTO webserver.users (username, password, email_address, created_time, updated_time) 
                    VALUES (:username, :password, :email, NOW(), NOW())
                ''')
                conn.execute(
                    insert_query, 
                    {
                        'username': username, 
                        'password': password,
                        'email': email
                    }
                )
                conn.commit()
                return True

        except SQLAlchemyError as error:
            print(f"Database error: {error}", file=sys.stderr)
            return False
        finally:
            if 'engine' in locals():
                engine.dispose()

class TestDatabaseOperations:
    def test_database_connection(self):
        """Test if we can connect to database and execute a simple query"""
        try:
            engine = DatabaseConnection.get_engine()
            
            # Test basic connection
            with engine.connect() as conn:
                # Test 1: Check if we can execute a simple query
                result = conn.execute(text("SELECT 1")).scalar()
                print(f"Basic connection test: {'SUCCESS' if result == 1 else 'FAILED'}")
                
                # Test 2: Check if we can query the users table
                result = conn.execute(text("SELECT COUNT(*) FROM webserver.users")).scalar()
                print(f"Found {result} users in the database")
                
                # Test 3: Check if we can retrieve actual user data
                sample_query = text("""
                    SELECT username, email_address, created_time 
                    FROM webserver.users 
                    LIMIT 1
                """)
                user_result = conn.execute(sample_query).fetchone()
                if user_result:
                    print("Sample user data:")
                    print(f"Username: {user_result.username}")
                    print(f"Email: {user_result.email_address}")
                    print(f"Created: {user_result.created_time}")
                else:
                    print("No users found in the database")
                
                return True
                
        except SQLAlchemyError as error:
            print(f"Database error: {error}", file=sys.stderr)
            return False
        finally:
            if 'engine' in locals():
                engine.dispose()

def main():
    """Main entry point for test script"""
    print("Running database tests...")
    test_db = TestDatabaseOperations()
    
    if len(sys.argv) > 1 and sys.argv[1] == "--verbose":
        # Run with detailed output
        result = test_db.test_database_connection()
        print(f"\nOverall test result: {'PASSED' if result else 'FAILED'}")
    else:
        # Simple test output
        try:
            result = test_db.test_database_connection()
            print(str(result).lower())
        except Exception as e:
            print(f"Test failed with error: {e}")
            print("false")

if __name__ == "__main__":
    main() 