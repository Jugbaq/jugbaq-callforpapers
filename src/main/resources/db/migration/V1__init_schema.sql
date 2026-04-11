-- =========================================
-- V1: Schema inicial multi-tenant
-- =========================================

-- Habilitamos extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "citext";  -- emails case-insensitive

-- =========================================
-- TENANTS (base del multi-tenancy)
-- =========================================
CREATE TABLE tenants (
                         id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         slug         VARCHAR(60) NOT NULL UNIQUE,
                         name         VARCHAR(120) NOT NULL,
                         logo_url     VARCHAR(500),
                         primary_color VARCHAR(20),
                         status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tenant inicial para JUGBAQ
INSERT INTO tenants (slug, name, primary_color)
VALUES ('jugbaq', 'Java User Group Barranquilla', '#E8622C');

-- =========================================
-- USERS & AUTH
-- =========================================
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           CITEXT NOT NULL UNIQUE,
                       password_hash   VARCHAR(255),  -- null si solo OAuth
                       full_name       VARCHAR(200) NOT NULL,
                       email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
                       status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un usuario pertenece a uno o más tenants con roles específicos
CREATE TABLE user_tenant_roles (
                                   user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                                   role       VARCHAR(30) NOT NULL,  -- SPEAKER, ORGANIZER, ADMIN
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   PRIMARY KEY (user_id, tenant_id, role)
);

CREATE INDEX idx_user_tenant_roles_tenant ON user_tenant_roles(tenant_id);

CREATE TABLE oauth_accounts (
                                id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                provider          VARCHAR(20) NOT NULL,  -- GOOGLE, GITHUB
                                provider_user_id  VARCHAR(200) NOT NULL,
                                created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user ON oauth_accounts(user_id);

-- =========================================
-- SPEAKER PROFILES (globales, no por tenant)
-- =========================================
CREATE TABLE speaker_profiles (
                                  user_id    UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                  tagline    VARCHAR(200),
                                  bio        TEXT,
                                  photo_url  VARCHAR(500),
                                  company    VARCHAR(150),
                                  job_title  VARCHAR(150),
                                  city       VARCHAR(100),
                                  country    VARCHAR(100),
                                  website_url VARCHAR(500),
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE speaker_social_links (
                                      id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      platform VARCHAR(20) NOT NULL,
                                      url      VARCHAR(500) NOT NULL
);

CREATE INDEX idx_speaker_social_links_user ON speaker_social_links(user_id);

-- =========================================
-- EVENTS (scoped por tenant)
-- =========================================
CREATE TABLE events (
                        id                         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        tenant_id                  UUID NOT NULL REFERENCES tenants(id),
                        slug                       VARCHAR(120) NOT NULL,
                        name                       VARCHAR(200) NOT NULL,
                        tagline                    VARCHAR(300),
                        description                TEXT,
                        event_date                 TIMESTAMPTZ NOT NULL,
                        location                   VARCHAR(300),
                        is_online                  BOOLEAN NOT NULL DEFAULT FALSE,
                        cfp_opens_at               TIMESTAMPTZ,
                        cfp_closes_at              TIMESTAMPTZ,
                        status                     VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        max_submissions_per_speaker INT NOT NULL DEFAULT 3,
                        created_by                 UUID NOT NULL REFERENCES users(id),
                        created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
                        UNIQUE (tenant_id, slug)
);

CREATE INDEX idx_events_tenant_status ON events(tenant_id, status);

CREATE TABLE event_tracks (
                              id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                              name        VARCHAR(100) NOT NULL,
                              description VARCHAR(300)
);

CREATE TABLE event_session_formats (
                                       id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                       event_id         UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                       name             VARCHAR(100) NOT NULL,
                                       duration_minutes INT NOT NULL
);

-- =========================================
-- SUBMISSIONS
-- =========================================
CREATE TABLE submissions (
                             id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             tenant_id    UUID NOT NULL REFERENCES tenants(id),
                             event_id     UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                             speaker_id   UUID NOT NULL REFERENCES users(id),
                             title        VARCHAR(250) NOT NULL,
                             abstract     TEXT NOT NULL,
                             pitch        TEXT,
                             level        VARCHAR(20) NOT NULL,  -- BEGINNER, INTERMEDIATE, ADVANCED
                             format_id    UUID REFERENCES event_session_formats(id),
                             track_id     UUID REFERENCES event_tracks(id),
                             status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                             submitted_at TIMESTAMPTZ,
                             created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_submissions_event_status ON submissions(event_id, status);
CREATE INDEX idx_submissions_speaker ON submissions(speaker_id);
CREATE INDEX idx_submissions_tenant ON submissions(tenant_id);

CREATE TABLE submission_tags (
                                 submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
                                 tag           VARCHAR(50) NOT NULL,
                                 PRIMARY KEY (submission_id, tag)
);

CREATE TABLE submission_co_speakers (
                                        submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
                                        user_id       UUID NOT NULL REFERENCES users(id),
                                        PRIMARY KEY (submission_id, user_id)
);

-- =========================================
-- REVIEW
-- =========================================
CREATE TABLE reviews (
                         id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
                         reviewer_id   UUID NOT NULL REFERENCES users(id),
                         score         SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
                         comment       TEXT,
                         created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                         tenant_id UUID NOT NULL REFERENCES tenants(id),
                         UNIQUE (submission_id, reviewer_id)
);

CREATE TABLE review_discussions (
                                    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
                                    author_id     UUID NOT NULL REFERENCES users(id),
                                    message       TEXT NOT NULL,
                                    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_discussions_submission ON review_discussions(submission_id);

-- =========================================
-- AGENDA
-- =========================================
CREATE TABLE agenda_slots (
                              id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              event_id       UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                              submission_id  UUID REFERENCES submissions(id),
                              track_id       UUID REFERENCES event_tracks(id),
                              starts_at      TIMESTAMPTZ NOT NULL,
                              ends_at        TIMESTAMPTZ NOT NULL,
                              title_override VARCHAR(200),
                              created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_agenda_slots_event ON agenda_slots(event_id, starts_at);

-- =========================================
-- NOTIFICATIONS
-- =========================================
CREATE TABLE notifications (
                               id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               tenant_id  UUID REFERENCES tenants(id),
                               type       VARCHAR(50) NOT NULL,
                               payload    JSONB NOT NULL DEFAULT '{}'::jsonb,
                               read_at    TIMESTAMPTZ,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE read_at IS NULL;

-- =========================================
-- SPRING MODULITH EVENT PUBLICATION
-- =========================================
CREATE TABLE event_publication (
                                   id               UUID NOT NULL,
                                   listener_id      TEXT NOT NULL,
                                   event_type       TEXT NOT NULL,
                                   serialized_event TEXT NOT NULL,
                                   publication_date TIMESTAMPTZ NOT NULL,
                                   completion_date  TIMESTAMPTZ,
                                   PRIMARY KEY (id)
);