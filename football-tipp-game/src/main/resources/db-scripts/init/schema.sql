-- =============================================================================
-- TIPPSPIEL - Database Schema (PostgreSQL)
-- =============================================================================
-- Multi-competition football betting game
-- Supports: FIFA World Cup, UEFA Euro Championship, and other tournaments
--
-- Author:  Werner Rodel
-- Created: 2026-05-10
-- Version: 1.0
-- =============================================================================

-- Enable extension for UUID generation (optional, used for invite codes)
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- =============================================================================
-- SECTION 1: TOURNAMENT STRUCTURE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- competition
-- Top-level entity for each football tournament (WM 2026, Euro 2028, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE competition (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,                       -- e.g. "FIFA World Cup 2026"
    type        VARCHAR(50)     NOT NULL,                       -- WORLD_CUP, EURO_CHAMPIONSHIP, etc.
    year        INT             NOT NULL,
    start_date  DATE            NOT NULL,
    end_date    DATE            NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'SETUP',       -- SETUP | ACTIVE | FINISHED
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_competition_status
        CHECK (status IN ('SETUP', 'ACTIVE', 'FINISHED')),
    CONSTRAINT chk_competition_dates
        CHECK (end_date > start_date)
);

COMMENT ON TABLE  competition            IS 'Football tournament (World Cup, Euro, etc.)';
COMMENT ON COLUMN competition.type       IS 'WORLD_CUP | EURO_CHAMPIONSHIP | NATIONS_LEAGUE | etc.';
COMMENT ON COLUMN competition.status     IS 'SETUP=configuring, ACTIVE=ongoing, FINISHED=done';


-- -----------------------------------------------------------------------------
-- team
-- National or club team (global, reused across competitions)
-- -----------------------------------------------------------------------------
CREATE TABLE team (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,                   -- e.g. "Switzerland"
    short_name      VARCHAR(10),                                -- e.g. "SUI"
    country_code    CHAR(3),                                    -- ISO 3166-1 alpha-3
    flag_url        VARCHAR(255)
);

COMMENT ON TABLE  team                  IS 'Football team (national or club), reused across competitions';
COMMENT ON COLUMN team.country_code     IS 'ISO 3166-1 alpha-3 code, e.g. SUI, GER, BRA';


-- -----------------------------------------------------------------------------
-- competition_team
-- Assignment of teams to a specific competition, including group placement
-- -----------------------------------------------------------------------------
CREATE TABLE competition_team (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL REFERENCES competition(id),
    team_id         BIGINT          NOT NULL REFERENCES team(id),
    group_name      CHAR(1),                                    -- 'A'-'F', NULL for knockout-only
    group_rank      INT,                                        -- 1-4, set after group phase

    CONSTRAINT uq_competition_team
        UNIQUE (competition_id, team_id),
    CONSTRAINT chk_group_rank
        CHECK (group_rank BETWEEN 1 AND 4)
);

COMMENT ON TABLE  competition_team              IS 'Teams participating in a specific competition';
COMMENT ON COLUMN competition_team.group_name   IS 'Group letter A-F; NULL for knockout-only competitions';
COMMENT ON COLUMN competition_team.group_rank   IS 'Final group ranking 1-4, set after group stage completes';


-- -----------------------------------------------------------------------------
-- venue
-- Stadium / venue where matches are played
-- -----------------------------------------------------------------------------
CREATE TABLE venue (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,                       -- e.g. "MetLife Stadium"
    city        VARCHAR(100)    NOT NULL,
    country     VARCHAR(100)    NOT NULL,
    capacity    INT
);

COMMENT ON TABLE venue IS 'Stadium or venue where matches are played';


-- -----------------------------------------------------------------------------
-- match
-- A single football match within a competition
-- Scores are NULL until actually played
-- -----------------------------------------------------------------------------
CREATE TABLE match (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL REFERENCES competition(id),
    match_number    INT             NOT NULL,                   -- sequential, e.g. 1-64 for WM
    stage           VARCHAR(20)     NOT NULL,                   -- GROUP | R16 | QF | SF | FINAL | THIRD_PLACE
    group_name      CHAR(1),                                    -- 'A'-'F', only for group stage
    match_date      TIMESTAMPTZ     NOT NULL,
    venue_id        BIGINT          REFERENCES venue(id),
    home_team_id    BIGINT          REFERENCES team(id),        -- NULL until knockout pairings known
    away_team_id    BIGINT          REFERENCES team(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | FINISHED

    -- Scores (all NULL until played)
    score_home_ht   INT,                                        -- half-time home goals
    score_away_ht   INT,                                        -- half-time away goals
    score_home_90   INT,                                        -- after 90 min
    score_away_90   INT,
    score_home_et   INT,                                        -- after extra time (120 min)
    score_away_et   INT,
    score_home_pen  INT,                                        -- after penalty shootout
    score_away_pen  INT,

    winner_team_id  BIGINT          REFERENCES team(id),        -- set when match finished

    CONSTRAINT uq_match_number
        UNIQUE (competition_id, match_number),
    CONSTRAINT chk_match_status
        CHECK (status IN ('SCHEDULED', 'FINISHED')),
    CONSTRAINT chk_match_stage
        CHECK (stage IN ('GROUP', 'R16', 'QF', 'SF', 'FINAL', 'THIRD_PLACE'))
);

COMMENT ON TABLE  match                 IS 'A single match within a competition';
COMMENT ON COLUMN match.match_number    IS 'Sequential match number within competition (e.g. 1-64 for WM)';
COMMENT ON COLUMN match.stage           IS 'GROUP | R16 (Round of 16) | QF (Quarter-final) | SF (Semi-final) | FINAL | THIRD_PLACE';
COMMENT ON COLUMN match.home_team_id    IS 'NULL until knockout pairings are determined';
COMMENT ON COLUMN match.score_home_et   IS 'Only set if match went to extra time';
COMMENT ON COLUMN match.score_home_pen  IS 'Only set if match went to penalty shootout';
COMMENT ON COLUMN match.winner_team_id  IS 'Set when match is finished; NULL for group stage draws';

CREATE INDEX idx_match_competition ON match(competition_id);
CREATE INDEX idx_match_date        ON match(match_date);
CREATE INDEX idx_match_stage       ON match(competition_id, stage);


-- =============================================================================
-- SECTION 2: USER MANAGEMENT & SECURITY
-- =============================================================================

-- -----------------------------------------------------------------------------
-- app_user
-- Registered user (Tipper). Passwords stored as BCrypt hash only.
-- Never store or send plain-text passwords.
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    nickname        VARCHAR(50)     NOT NULL UNIQUE,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    city            VARCHAR(100),
    password_hash   VARCHAR(255)    NOT NULL,                   -- BCrypt hash, never plain text
    is_admin        BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,     -- must verify before betting
    login_attempts  INT             NOT NULL DEFAULT 0,         -- reset on successful login
    locked_until    TIMESTAMPTZ,                                -- NULL = not locked; 3 fails = 5 min lock
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ
);

COMMENT ON TABLE  app_user                  IS 'Registered user (Tipper). Replaces old TIPPER table.';
COMMENT ON COLUMN app_user.password_hash    IS 'BCrypt hash. NEVER store or transmit plain-text passwords.';
COMMENT ON COLUMN app_user.email_verified   IS 'User must click email link within 48h to activate account.';
COMMENT ON COLUMN app_user.login_attempts   IS 'Failed login counter. Reset to 0 on successful login.';
COMMENT ON COLUMN app_user.locked_until     IS 'Account locked after 3 failed attempts for 5 minutes.';

CREATE INDEX idx_app_user_email    ON app_user(email);
CREATE INDEX idx_app_user_nickname ON app_user(nickname);


-- -----------------------------------------------------------------------------
-- email_verification_token
-- Token sent by email at registration. Must be confirmed within 48 hours.
-- Unconfirmed registrations are cleaned up by a scheduled job.
-- -----------------------------------------------------------------------------
CREATE TABLE email_verification_token (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    NOT NULL UNIQUE,                -- SHA-256 hash of token
    expires_at  TIMESTAMPTZ     NOT NULL,                       -- 48 hours after creation
    used_at     TIMESTAMPTZ                                     -- NULL = not yet used
);

COMMENT ON TABLE  email_verification_token              IS 'Email verification token sent at registration (48h expiry)';
COMMENT ON COLUMN email_verification_token.token_hash   IS 'SHA-256 hash of the token sent in email link';
COMMENT ON COLUMN email_verification_token.expires_at   IS 'Token expires 48 hours after creation. Cleanup job deletes expired unverified users.';

CREATE INDEX idx_email_token_user    ON email_verification_token(user_id);
CREATE INDEX idx_email_token_expires ON email_verification_token(expires_at);


-- -----------------------------------------------------------------------------
-- password_reset_token
-- Token sent by email for password reset. Valid for 2 hours.
-- Replaces old insecure "send plain password" approach.
-- -----------------------------------------------------------------------------
CREATE TABLE password_reset_token (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    NOT NULL UNIQUE,                -- SHA-256 hash of token
    expires_at  TIMESTAMPTZ     NOT NULL,                       -- 2 hours after creation
    used_at     TIMESTAMPTZ                                     -- NULL = not yet used
);

COMMENT ON TABLE  password_reset_token              IS 'Password reset token sent by email (2h expiry). Replaces old plain-text password email.';
COMMENT ON COLUMN password_reset_token.token_hash   IS 'SHA-256 hash of the reset token. Raw token only in email, never stored.';

CREATE INDEX idx_pw_reset_user    ON password_reset_token(user_id);
CREATE INDEX idx_pw_reset_expires ON password_reset_token(expires_at);


-- =============================================================================
-- SECTION 3: TIPPING STRUCTURE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tipp_group
-- A group of friends betting together within one competition.
-- Multiple tipp_groups can exist per competition (e.g. friends, colleagues).
-- Auto-locked when competition starts.
-- -----------------------------------------------------------------------------
CREATE TABLE tipp_group (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL REFERENCES competition(id),
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    created_by      BIGINT          NOT NULL REFERENCES app_user(id),
    invite_code     VARCHAR(20)     NOT NULL UNIQUE,            -- short code to join group
    is_locked       BOOLEAN         NOT NULL DEFAULT FALSE,     -- auto-locked at competition start
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tipp_group_name
        UNIQUE (competition_id, name)
);

COMMENT ON TABLE  tipp_group                IS 'A betting group within one competition. Replaces old TIPP_TEAM table.';
COMMENT ON COLUMN tipp_group.invite_code    IS 'Short alphanumeric code shared with friends to join group.';
COMMENT ON COLUMN tipp_group.is_locked      IS 'True = no new members allowed. Auto-set when competition starts.';

CREATE INDEX idx_tipp_group_competition ON tipp_group(competition_id);
CREATE INDEX idx_tipp_group_invite      ON tipp_group(invite_code);


-- -----------------------------------------------------------------------------
-- tipp_group_member
-- Membership of a user in a tipp_group, including score tracking.
-- Replaces old MITGLIEDSCHAFT table.
-- -----------------------------------------------------------------------------
CREATE TABLE tipp_group_member (
    id                  BIGSERIAL       PRIMARY KEY,
    tipp_group_id       BIGINT          NOT NULL REFERENCES tipp_group(id),
    user_id             BIGINT          NOT NULL REFERENCES app_user(id),
    is_admin            BOOLEAN         NOT NULL DEFAULT FALSE, -- TippTeamChef
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | REMOVED | BANNED
    joined_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    total_points        INT             NOT NULL DEFAULT 0,     -- current total score
    total_points_prev   INT             NOT NULL DEFAULT 0,     -- score before last recalculation (for trend)
    rank                INT,                                    -- current rank within group
    rank_prev           INT,                                    -- rank before last recalculation (for trend arrow)

    CONSTRAINT uq_tipp_group_member
        UNIQUE (tipp_group_id, user_id),
    CONSTRAINT chk_member_status
        CHECK (status IN ('ACTIVE', 'REMOVED', 'BANNED'))
);

COMMENT ON TABLE  tipp_group_member                     IS 'User membership in a tipp group. Replaces old MITGLIEDSCHAFT table.';
COMMENT ON COLUMN tipp_group_member.is_admin            IS 'True = TippTeamChef; can manage members and scoring rules.';
COMMENT ON COLUMN tipp_group_member.status              IS 'ACTIVE=normal, REMOVED=left/removed (can rejoin), BANNED=excluded (cannot rejoin).';
COMMENT ON COLUMN tipp_group_member.total_points_prev   IS 'Points before last recalculation, used to display trend arrow (up/down/same).';
COMMENT ON COLUMN tipp_group_member.rank_prev           IS 'Rank before last recalculation, used to display trend arrow.';

CREATE INDEX idx_tgm_group  ON tipp_group_member(tipp_group_id);
CREATE INDEX idx_tgm_user   ON tipp_group_member(user_id);
CREATE INDEX idx_tgm_points ON tipp_group_member(tipp_group_id, total_points DESC);


-- -----------------------------------------------------------------------------
-- tip
-- A user's score prediction for one match (Resultat-Tipp).
-- Can be submitted or changed until 10 minutes before kick-off.
-- -----------------------------------------------------------------------------
CREATE TABLE tip (
    id                      BIGSERIAL       PRIMARY KEY,
    tipp_group_member_id    BIGINT          NOT NULL REFERENCES tipp_group_member(id),
    match_id                BIGINT          NOT NULL REFERENCES match(id),
    score_home              INT             NOT NULL,           -- predicted home goals
    score_away              INT             NOT NULL,           -- predicted away goals
    result_after            VARCHAR(10),                        -- NULL=group stage | ET=extra time | PENALTY
    submitted_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tip
        UNIQUE (tipp_group_member_id, match_id),
    CONSTRAINT chk_tip_result_after
        CHECK (result_after IS NULL OR result_after IN ('ET', 'PENALTY')),
    CONSTRAINT chk_tip_scores
        CHECK (score_home >= 0 AND score_away >= 0)
);

COMMENT ON TABLE  tip                   IS 'Score prediction for one match (Resultat-Tipp). Locked 10 min before kick-off.';
COMMENT ON COLUMN tip.result_after      IS 'For knockout matches: NULL=group stage, ET=extra time, PENALTY=penalties. Group tips are always NULL.';
COMMENT ON COLUMN tip.score_home        IS 'Predicted goals for home team (>= 0)';
COMMENT ON COLUMN tip.score_away        IS 'Predicted goals for away team (>= 0)';

CREATE INDEX idx_tip_member ON tip(tipp_group_member_id);
CREATE INDEX idx_tip_match  ON tip(match_id);


-- -----------------------------------------------------------------------------
-- tip_points
-- Points awarded per tip, broken down by rule type.
-- Recalculated after each match result is entered.
-- Replaces old RESULTAT_TIPP_PUNKTE table.
-- -----------------------------------------------------------------------------
CREATE TABLE tip_points (
    id                      BIGSERIAL       PRIMARY KEY,
    tip_id                  BIGINT          NOT NULL REFERENCES tip(id) ON DELETE CASCADE,
    tipp_group_member_id    BIGINT          NOT NULL REFERENCES tipp_group_member(id),
    rule_type               VARCHAR(30)     NOT NULL,           -- TOTO | GOAL_DIFF | EXACT | WINNER | TIMING
    points                  INT             NOT NULL DEFAULT 0,
    calculated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tip_points
        UNIQUE (tip_id, rule_type),
    CONSTRAINT chk_tip_points_rule_type
        CHECK (rule_type IN ('TOTO', 'GOAL_DIFF', 'EXACT', 'WINNER', 'TIMING'))
);

COMMENT ON TABLE  tip_points                IS 'Points per tip broken down by rule. Replaces old RESULTAT_TIPP_PUNKTE table.';
COMMENT ON COLUMN tip_points.rule_type      IS 'TOTO=1X2 result, GOAL_DIFF=goal difference, EXACT=exact score, WINNER=knockout winner, TIMING=90min/ET/penalty';

CREATE INDEX idx_tip_points_member ON tip_points(tipp_group_member_id);
CREATE INDEX idx_tip_points_tip    ON tip_points(tip_id);


-- -----------------------------------------------------------------------------
-- prognosis_tip
-- Tournament-wide predictions submitted before competition starts (Prognose-Tipp).
-- Frozen at competition kick-off, cannot be changed afterwards.
-- -----------------------------------------------------------------------------
CREATE TABLE prognosis_tip (
    id                      BIGSERIAL       PRIMARY KEY,
    tipp_group_member_id    BIGINT          NOT NULL REFERENCES tipp_group_member(id),
    competition_id          BIGINT          NOT NULL REFERENCES competition(id),
    tip_type                VARCHAR(30)     NOT NULL,           -- see comment below
    team_id                 BIGINT          NOT NULL REFERENCES team(id),
    group_name              CHAR(1),                            -- only for GROUP_RANK tips
    predicted_rank          INT,                                -- only for GROUP_RANK tips (1-4)
    submitted_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    points_earned           INT             NOT NULL DEFAULT 0,

    CONSTRAINT chk_prognosis_tip_type
        CHECK (tip_type IN (
            'GROUP_RANK_1', 'GROUP_RANK_2', 'GROUP_RANK_3', 'GROUP_RANK_4',
            'ADVANCE_R16', 'ADVANCE_QF', 'ADVANCE_SF', 'WINNER'
        ))
);

COMMENT ON TABLE  prognosis_tip                 IS 'Pre-tournament predictions (Prognose-Tipp). Frozen at competition start.';
COMMENT ON COLUMN prognosis_tip.tip_type        IS 'GROUP_RANK_1..4=group rankings, ADVANCE_R16/QF/SF=advancement picks, WINNER=champion';
COMMENT ON COLUMN prognosis_tip.group_name      IS 'Which group (A-F) this ranking prediction is for. Only for GROUP_RANK tips.';
COMMENT ON COLUMN prognosis_tip.predicted_rank  IS 'Predicted rank 1-4 within group. Only for GROUP_RANK tips.';

CREATE INDEX idx_prognosis_member      ON prognosis_tip(tipp_group_member_id);
CREATE INDEX idx_prognosis_competition ON prognosis_tip(competition_id);


-- =============================================================================
-- SECTION 4: SCORING RULES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- scoring_rule
-- Points awarded per rule type and match stage, configurable per tipp_group.
-- Each group inherits default rules and the TippTeamChef can adjust points.
-- Setting points = 0 disables that rule for the group.
-- -----------------------------------------------------------------------------
CREATE TABLE scoring_rule (
    id              BIGSERIAL       PRIMARY KEY,
    tipp_group_id   BIGINT          NOT NULL REFERENCES tipp_group(id) ON DELETE CASCADE,
    rule_type       VARCHAR(30)     NOT NULL,
    match_stage     VARCHAR(20)     NOT NULL,
    points          INT             NOT NULL DEFAULT 0,
    points_min      INT             NOT NULL DEFAULT 0,         -- min adjustable value (0 = can disable)
    points_max      INT             NOT NULL DEFAULT 20,        -- max adjustable value

    CONSTRAINT uq_scoring_rule
        UNIQUE (tipp_group_id, rule_type, match_stage),
    CONSTRAINT chk_scoring_rule_type
        CHECK (rule_type IN (
            -- Result tips (Resultat-Tipp)
            'TOTO',         -- 1X2 correct (group stage only)
            'GOAL_DIFF',    -- correct goal difference
            'EXACT',        -- exact score correct
            'WINNER',       -- correct winner (knockout)
            'TIMING',       -- correct timing (90min / ET / penalty)
            -- Prognosis tips (Prognose-Tipp)
            'GROUP_RANK_1', -- correct 1st place in group
            'GROUP_RANK_2', -- correct 2nd place in group
            'GROUP_RANK_3', -- correct 3rd place in group
            'GROUP_RANK_4', -- correct 4th place in group
            'ADVANCEMENT'   -- correct team advancing to next round
        )),
    CONSTRAINT chk_scoring_rule_stage
        CHECK (match_stage IN ('GROUP', 'R16', 'QF', 'SF', 'FINAL')),
    CONSTRAINT chk_scoring_rule_points
        CHECK (points >= 0 AND points_min >= 0 AND points_max >= points_min)
);

COMMENT ON TABLE  scoring_rule              IS 'Points per rule type and stage, configurable per tipp_group. Replaces old REGEL_WERK / REGEL tables.';
COMMENT ON COLUMN scoring_rule.rule_type    IS 'TOTO/GOAL_DIFF/EXACT/WINNER/TIMING for result tips; GROUP_RANK_1..4/ADVANCEMENT for prognosis tips.';
COMMENT ON COLUMN scoring_rule.match_stage  IS 'GROUP | R16 | QF | SF | FINAL';
COMMENT ON COLUMN scoring_rule.points       IS 'Current points for this rule. 0 = rule disabled for this group.';
COMMENT ON COLUMN scoring_rule.points_min   IS 'Minimum value TippTeamChef can set (usually 0).';
COMMENT ON COLUMN scoring_rule.points_max   IS 'Maximum value TippTeamChef can set.';

CREATE INDEX idx_scoring_rule_group ON scoring_rule(tipp_group_id);


-- =============================================================================
-- SECTION 5: DEFAULT SCORING RULES (Seed Data)
-- =============================================================================
-- Default rule values based on the original EM-Tippspiel requirements.
-- Applied to new tipp_groups on creation via application logic.
-- =============================================================================

-- Default scoring rules reference table (not per group, used as template)
CREATE TABLE default_scoring_rule (
    id          BIGSERIAL       PRIMARY KEY,
    rule_type   VARCHAR(30)     NOT NULL,
    match_stage VARCHAR(20)     NOT NULL,
    points      INT             NOT NULL,
    points_min  INT             NOT NULL DEFAULT 0,
    points_max  INT             NOT NULL,

    CONSTRAINT uq_default_scoring_rule
        UNIQUE (rule_type, match_stage)
);

COMMENT ON TABLE default_scoring_rule IS 'Template scoring rules used when creating new tipp_groups. Based on original requirements.';

-- Result tips: Group stage
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('TOTO',        'GROUP',  1,  0,  5),
    ('GOAL_DIFF',   'GROUP',  1,  0,  5),
    ('EXACT',       'GROUP',  1,  0,  5);

-- Result tips: Round of 16
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('WINNER',  'R16',  2,  0, 10),
    ('TIMING',  'R16',  2,  0, 10),
    ('EXACT',   'R16',  2,  0, 10);

-- Result tips: Quarter-final
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('WINNER',  'QF',   2,  0, 10),
    ('TIMING',  'QF',   2,  0, 10),
    ('EXACT',   'QF',   2,  0, 10);

-- Result tips: Semi-final
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('WINNER',  'SF',   3,  0, 15),
    ('TIMING',  'SF',   3,  0, 15),
    ('EXACT',   'SF',   3,  0, 15);

-- Result tips: Final
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('WINNER',  'FINAL',  5,  0, 20),
    ('TIMING',  'FINAL',  5,  0, 20),
    ('EXACT',   'FINAL',  5,  0, 20);

-- Prognosis tips: Group rankings (all stages use 'GROUP')
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('GROUP_RANK_1',  'GROUP',  4,  0, 10),
    ('GROUP_RANK_2',  'GROUP',  4,  0, 10),
    ('GROUP_RANK_3',  'GROUP',  2,  0,  8),
    ('GROUP_RANK_4',  'GROUP',  2,  0,  8);

-- Prognosis tips: Advancement picks
INSERT INTO default_scoring_rule (rule_type, match_stage, points, points_min, points_max) VALUES
    ('ADVANCEMENT', 'R16',    2,  0, 10),
    ('ADVANCEMENT', 'QF',     5,  0, 15),
    ('ADVANCEMENT', 'SF',    10,  0, 20),
    ('ADVANCEMENT', 'FINAL', 20,  0, 30);


-- =============================================================================
-- SECTION 6: INDEXES (additional performance indexes)
-- =============================================================================

CREATE INDEX idx_competition_status    ON competition(status);
CREATE INDEX idx_match_home_team       ON match(home_team_id);
CREATE INDEX idx_match_away_team       ON match(away_team_id);
CREATE INDEX idx_app_user_locked_until ON app_user(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX idx_ev_token_expires      ON email_verification_token(expires_at) WHERE used_at IS NULL;
CREATE INDEX idx_pw_token_expires      ON password_reset_token(expires_at)     WHERE used_at IS NULL;


-- =============================================================================
-- END OF SCHEMA
-- =============================================================================
