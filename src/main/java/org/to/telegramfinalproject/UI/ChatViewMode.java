package org.to.telegramfinalproject.UI;


//For search handling
public enum ChatViewMode {
    NORMAL,                 // member/contact; can send messages
    NEEDS_JOIN,             // group/channel preview; show Join button
    NEEDS_ADD_CONTACT,      // private preview; show Add Contact button
    READ_ONLY,
    BLOCKED
    }

