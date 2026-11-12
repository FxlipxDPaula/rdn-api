-- Usuarios de desenvolvimento (Etapa 2 - seguranca).
--
-- Senhas em texto puro existem apenas para facilitar testes locais; o banco
-- guarda somente o hash BCrypt (forca 10), nunca a senha. Ver README.
--
--   admin@rdn.local     / Admin@123      -> ADMIN
--   staff@rdn.local     / Staff@123      -> STAFF
--   kitchen@rdn.local   / Kitchen@123    -> KITCHEN
--   customer@rdn.local  / Customer@123   -> CUSTOMER
--
-- Os hashes abaixo foram gerados com BCryptPasswordEncoder.encode(...).
-- Se uma senha mudar, gere um novo hash com o mesmo componente: nunca edite
-- o valor do hash a mao.

INSERT INTO users (id, full_name, email, password_hash, role, is_active)
VALUES ('00000000-0000-0000-0000-00000000000c', 'Administrador RDN', 'admin@rdn.local',
        '$2a$10$umwFvf2QUeCqAj5hA2K4suoVwkBOmTyPbSgcKwtX5tHp0XgOl1Rwu', 'ADMIN', TRUE),
       ('00000000-0000-0000-0000-00000000000d', 'Atendente RDN', 'staff@rdn.local',
        '$2a$10$pmPAf01ZODPj2vLsk0E3vOLUPEe52Xs8mictOZjlAe2WEsRTjlFwa', 'STAFF', TRUE),
       ('00000000-0000-0000-0000-00000000000e', 'Cozinha RDN', 'kitchen@rdn.local',
        '$2a$10$ZH8l4PxDm0e/O5Un6pXU6.LhXNTbo5v94SKoFGsDR37e2k8YQN8EW', 'KITCHEN', TRUE),
       ('00000000-0000-0000-0000-00000000000f', 'Cliente RDN', 'customer@rdn.local',
        '$2a$10$WloepsL2pIT7iPzjPCFA6./xN70irCZSq08k7K0pNTKCF17g9dWOq', 'CUSTOMER', TRUE);
