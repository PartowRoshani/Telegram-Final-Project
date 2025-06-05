CREATE TABLE IF NOT EXISTS users(
                                    user_id VARCHAR(70) UNIQUE,
    internal_uuid UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    profile_name VARCHAR(100),
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
    receiver_id UUID NOT NULL,
    content TEXT,
    message_type VARCHAR(20) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT','IMAGE','FILE','VIDEO','AUDIO','STICKER','GIF')),
    file_url TEXT,                                                --(Bouns)
    send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20),                                      -- SEND,DELIVERED,READ
    reply_to_id UUID REFERENCES messages(message_id) ON DELETE SET NULL,        --(Bouns)
    is_edited BOOLEAN,                                           --(Bonus)
    original_message_id UUID REFERENCES message_id,              --(Bonus)
    forwarded_by UUID REFERENCES users(internal_uuid),              --(Bouns)
    forwarded_from UUID REFERENCES users(internal_uuid)              --(Bouns)
    );


CREATE TABLE IF NOT EXISTS private_chat (
                                            chat_id UUID PRIMARY KEY,
                                            user1-id UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,
    user2_id UUID REFERENCES users(internal_uuid) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_users UNIQUE (user1_id, user2_id)
    );


CREATE TABLE IF NOT EXISTS groups (
                                      group_id VARCHAR(70) UNIQUE,
    internal_uuid UUID,
    group_name VARCHAR(100),
    creator_id UUID REFERENCES users(internal_uuid),
    image_url TEXT,
    description TEXT,
    PRIMARY KEY (group_id, internal_uuid)
    );


CREATE TABLE IF NOT EXISTS group_members (
                                             group_id UUID REFERENCES groups(group_id),
    user_id UUID REFERENCES users(internal_uuid),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'member',                  -- member/admin/owner
    PRIMARY KEY (group_id, user_id)
    );


CREATE TABLE IF NOT EXISTS channels (
                                        channel_id VARCHAR(70) UNIQUE,
    internal_uuid UUID,
    channel_name VARCHAR(100),
    creator_id UUID REFERENCES users(internal_uuid),
    image_url TEXT,
    description TEXT,
    PRIMARY KEY (channel_id, internal_uuid)
    );


CREATE TABLE IF NOT EXISTS channel_subscribers (
                                                   channel_id UUID REFERENCES channels(channel_id),
    user_id UUID REFERENCES users(internal_uuid),
    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (channel_id, user_id)
    );


--(Bouns)
CREATE TABLE IF NOT EXISTS channel_admins (
                                              channel_id UUID REFERENCES channels(channel_id),
    user_id UUID REFERENCES users(internal_uuid),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by UUID REFERENCES users(internal_uuid),
    role VARCHAR(20) DEFAULT 'admin',              --owner, admin
    can_post BOOLEAN DEFAULT TRUE,
    can_edit BOOLEAN DEFAULT FALSE,
    can_delete_messages BOOLEAN DEFAULT FALSE,
    can_delete_members BOOLEAN DEFAULT FALSE,
    can_add_members BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (channel_id, user_id)
    );