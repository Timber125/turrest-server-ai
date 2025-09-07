CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE OR REPLACE FUNCTION public.random_uuid() RETURNS UUID
    AS 'select public.uuid_generate_v4()'
    LANGUAGE SQL;