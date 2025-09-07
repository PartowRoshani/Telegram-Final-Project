CREATE TABLE IF NOT EXISTS users(
    user_id VARCHAR(70) UNIQUE NOT NULL,
    internal_uuid UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    profile_name VARCHAR(100) NOT NULL,
    bio TEXT,                               --(Bonus)
    image_url TEXT,
    status VARCHAR(20) DEFAULT 'offline',
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS contacts (
    user_id UUID REFERENCES users(internal_uuid) ON DELETE CASCADE,
    contact_id UUID REFERENCES users(internal_uuid) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_blocked BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, contact_id)
    );


CREATE TABLE IF NOT EXISTS messages (
    message_id UUID PRIMARY KEY,
    sender_id UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,
    receiver_type VARCHAR(20) NOT NULL CHECK (receiver_type IN ('private','group','channel')),
    receiver_id UUID NOT NULL ,
    content TEXT,
    message_type VARCHAR(20) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT','IMAGE','FILE','VIDEO','AUDIO','STICKER','GIF')),
    send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SEND',                       -- SEND,DELIVERED,READ
    reply_to_id UUID REFERENCES messages(message_id) ON DELETE SET NULL,        --(Bouns)
    is_edited BOOLEAN DEFAULT FALSE,                                            --(Bonus)
    edited_at TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
    original_message_id UUID REFERENCES messages(message_id) ON DELETE SET NULL,              --(Bonus)
    forwarded_by UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,              --(Bouns)
    forwarded_from UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,            --(Bouns)
    is_deleted_globally BOOLEAN DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS private_chat (
     chat_id UUID PRIMARY KEY,
    user1_id UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,
    user2_id UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,
    user1_deleted BOOLEAN DEFAULT FALSE,
    user2_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_users UNIQUE (user1_id, user2_id)
    );


CREATE TABLE IF NOT EXISTS groups (
    internal_uuid UUID PRIMARY KEY,
    group_id VARCHAR(70) UNIQUE,
    group_name VARCHAR(100),
    creator_id UUID REFERENCES users(internal_uuid),
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);

CREATE TABLE group_members (
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role TEXT CHECK (role IN ('owner', 'admin', 'member')) DEFAULT 'member',
    permissions JSONB DEFAULT '{}'::jsonb, -- مخصوص adminها
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);


CREATE TABLE IF NOT EXISTS channels (
    channel_id VARCHAR(70) UNIQUE,
    internal_uuid UUID PRIMARY KEY,
    channel_name VARCHAR(100),
    creator_id UUID REFERENCES users(internal_uuid),
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT
    
    );


CREATE TABLE channel_subscribers (
    channel_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role TEXT CHECK (role IN ('owner', 'admin', 'subscriber')) DEFAULT 'subscriber',
    permissions JSONB DEFAULT '{}'::jsonb,
    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (channel_id, user_id)
);



CREATE TABLE IF NOT EXISTS message_receipts (
    message_id UUID REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(internal_uuid) ON DELETE CASCADE,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);

--(Bouns)
CREATE TABLE archived_chats (
    user_id UUID NOT NULL REFERENCES users(internal_uuid) ON DELETE CASCADE,
    chat_id UUID NOT NULL,
    chat_type TEXT NOT NULL CHECK (chat_type IN ('private', 'group', 'channel')),
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, chat_id)
);


CREATE TABLE deleted_messages (
    message_id UUID REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(internal_uuid) ON DELETE CASCADE,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);


CREATE TABLE IF NOT EXISTS message_reactions (
    message_id UUID REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(internal_uuid) ON DELETE CASCADE,
    emoji TEXT NOT NULL,  
    reacted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);

--Multi attachments
CREATE TABLE IF NOT EXISTS message_attachments (
    attachment_id UUID PRIMARY KEY,
    message_id UUID REFERENCES messages(message_id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_type VARCHAR(20) CHECK (file_type IN ('IMAGE','VIDEO','AUDIO','FILE','GIF','STICKER')),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_name TEXT,
    file_size BIGINT,
    mime_type TEXT,
    width INT,
    height INT,
    duration_seconds INT,
    thumbnail_url TEXT

);

--Run this part in your pg
ALTER TABLE message_attachments
  ADD COLUMN IF NOT EXISTS media_key UUID;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE message_attachments
SET media_key = gen_random_uuid()
WHERE media_key IS NULL;



