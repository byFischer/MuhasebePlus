ALTER TABLE "user" ALTER COLUMN user_role TYPE VARCHAR(50);
DROP TYPE user_role;
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
ALTER TABLE "user"
    ALTER COLUMN user_role TYPE user_role
    USING upper(user_role)::user_role;
ALTER TABLE "user" ALTER COLUMN user_role SET DEFAULT 'USER';
