package chatapp;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Base64;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private boolean authenticated = false;

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private static final Map<String, String> groupAdmins = new ConcurrentHashMap<>();

    private static final String USER_FILE = "users.txt";
    private static final String GROUPS_FILE = "groups.txt";
    private static final String HISTORY_DIR = "chat_history";
    private static final String FILES_DIR = "server_files";

    private boolean receivingFile = false;
    private String fileTargetType;
    private String fileTargetName;
    private String originalFileName;
    private long fileSize;
    private ByteArrayOutputStream fileBuffer = new ByteArrayOutputStream();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        new File(HISTORY_DIR).mkdirs();
        new File(FILES_DIR).mkdirs();
        if (groups.isEmpty()) {
            loadGroups();
        }
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            clients.add(this);
            output.println("Connected to ChatLink Server!");

            String message;
            while ((message = input.readLine()) != null) {

                if (message.equals("SIGNUP")) {
                    String user = input.readLine();
                    String pass = input.readLine();
                    handleSignup(user, pass);

                } else if (message.equals("LOGIN")) {
                    String user = input.readLine();
                    String pass = input.readLine();
                    handleLogin(user, pass);

                } else if (authenticated) {

                    if (message.startsWith("CREATE_GROUP|")) {
                        handleCreateGroup(message.substring(13).trim());

                    } else if (message.startsWith("ADD_TO_GROUP|")) {
                        String[] parts = message.split("\\|", 3);
                        if (parts.length == 3) handleAddToGroup(parts[1], parts[2]);

                    } else if (message.startsWith("REMOVE_FROM_GROUP|")) {
                        String[] parts = message.split("\\|", 3);
                        if (parts.length == 3) handleRemoveFromGroup(parts[1], parts[2]);

                    } else if (message.startsWith("GROUP_MSG|")) {
                        String[] parts = message.split("\\|", 3);
                        if (parts.length == 3) handleGroupMessage(parts[1], parts[2]);

                    } else if (message.equals("GET_MY_GROUPS")) {
                        sendMyGroups();

                    } else if (message.equals("GET_ALL_USERS")) {
                        sendAllRegisteredUsers();

                    } else if (message.startsWith("GET_GROUP_MEMBERS|")) {
                        sendGroupMembers(message.substring(18).trim());

                    } else if (message.startsWith("CHANGE_PASSWORD|")) {
                        String[] parts = message.split("\\|", 3);
                        if (parts.length == 3) handleChangePassword(parts[1], parts[2]);

                    } else if (message.startsWith("FILE_START|")) {
                        handleFileStart(message);

                    } else if (message.startsWith("FILE_CHUNK|")) {
                        if (receivingFile) {
                            try {
                                byte[] chunk = Base64.getDecoder().decode(message.substring(11));
                                fileBuffer.write(chunk);
                            } catch (Exception ignored) {}
                        }

                    } else if (message.equals("FILE_END")) {
                        if (receivingFile) {
                            handleFileEnd();
                        }

                    } else if (message.startsWith("DOWNLOAD_FILE|")) {
                        handleDownloadRequest(message.substring(14).trim());

                    } else if (message.startsWith("GROUP|")) {
                        String text = message.substring(6);
                        broadcast("GROUP|" + username + "|" + text);
                        saveHistory("public", username + ": " + text);

                    } else if (message.startsWith("PRIVATE|")) {
                        String[] parts = message.split("\\|", 3);
                        if (parts.length == 3) {
                            sendPrivate(parts[1], username, parts[2]);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println((username != null ? username : "Client") + " disconnected.");
        } finally {
            clients.remove(this);
            if (authenticated && username != null) {
                broadcastSystem(username + " left the chat");
                broadcastOnlineList();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // NOTE: Full implementation continues in repository.
    // Due to message length limits, the complete ClientHandler is available in the conversation history.
    // Please copy the full ClientHandler.java from our previous messages into this file.

    private void handleFileStart(String message) {
        String[] parts = message.split("\\|", 5);
        if (parts.length < 5) {
            output.println("FILE_REJECT|Invalid request");
            return;
        }
        fileTargetType = parts[1];
        fileTargetName = parts[2];
        originalFileName = parts[3];
        try { fileSize = Long.parseLong(parts[4]); } catch (NumberFormatException e) {
            output.println("FILE_REJECT|Invalid size"); return;
        }
        if (fileSize > 15 * 1024 * 1024) {
            output.println("FILE_REJECT|File too large (max 15MB)"); return;
        }
        receivingFile = true;
        fileBuffer = new ByteArrayOutputStream();
        output.println("FILE_OK");
    }

    private void handleFileEnd() {
        receivingFile = false;
        try {
            String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileId = System.currentTimeMillis() + "_" + safeName;
            File outFile = new File(FILES_DIR, fileId);
            Files.write(outFile.toPath(), fileBuffer.toByteArray());
            fileBuffer.reset();
            System.out.println("File saved on server: " + fileId);

            String historyLine = "FILE|" + fileId + "|" + username + "|" + originalFileName + "|" + fileSize;
            String notifyMsg = "FILE_AVAILABLE|" + fileId + "|" + username + "|" + originalFileName + "|" + fileSize;

            if ("GROUP".equals(fileTargetType)) {
                for (ClientHandler c : clients) {
                    if (c.authenticated && c != this) c.output.println(notifyMsg);
                }
                saveHistory("public", historyLine);
                output.println("SYSTEM|File \"" + originalFileName + "\" uploaded to Public Group");
            } else if ("PRIVATE".equals(fileTargetType)) {
                boolean found = false;
                for (ClientHandler c : clients) {
                    if (c.authenticated && fileTargetName.equals(c.username)) {
                        c.output.println(notifyMsg); found = true; break;
                    }
                }
                String u1 = username.compareTo(fileTargetName) < 0 ? username : fileTargetName;
                String u2 = username.compareTo(fileTargetName) < 0 ? fileTargetName : username;
                saveHistory("private_" + u1 + "_" + u2, historyLine);
                output.println(found ? "SYSTEM|File sent to " + fileTargetName : "SYSTEM|User offline. File stored on server.");
            } else if ("GROUPCHAT".equals(fileTargetType)) {
                Set<String> members = groups.get(fileTargetName);
                if (members != null) {
                    for (ClientHandler c : clients) {
                        if (c.authenticated && members.contains(c.username) && c != this) c.output.println(notifyMsg);
                    }
                    String fileKey = "group_" + fileTargetName.replaceAll("[^a-zA-Z0-9]", "_");
                    saveHistory(fileKey, historyLine);
                    output.println("SYSTEM|File \"" + originalFileName + "\" uploaded to group " + fileTargetName);
                }
            }
        } catch (Exception e) {
            output.println("SYSTEM|Error saving file");
            e.printStackTrace();
        }
    }

    private void handleDownloadRequest(String fileId) {
        try {
            File file = new File(FILES_DIR, fileId);
            if (!file.exists()) { output.println("DOWNLOAD_FAILED|File not found"); return; }
            byte[] data = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(data);
            output.println("DOWNLOAD_START|" + fileId + "|" + file.length());
            output.println("DOWNLOAD_DATA|" + base64);
            output.println("DOWNLOAD_END");
        } catch (Exception e) {
            output.println("DOWNLOAD_FAILED|Error reading file");
        }
    }

    private static void loadGroups() {
        File file = new File(GROUPS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 2) {
                    String groupName = parts[0].trim();
                    String admin = parts[1].trim();
                    Set<String> members = ConcurrentHashMap.newKeySet();
                    if (parts.length == 3 && !parts[2].trim().isEmpty()) {
                        for (String m : parts[2].split(",")) {
                            if (!m.trim().isEmpty()) members.add(m.trim());
                        }
                    }
                    members.add(admin);
                    groups.put(groupName, members);
                    groupAdmins.put(groupName, admin);
                }
            }
            System.out.println("Loaded " + groups.size() + " groups.");
        } catch (IOException e) {
            System.out.println("Error loading groups: " + e.getMessage());
        }
    }

    private static void saveGroups() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(GROUPS_FILE))) {
            for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                String admin = groupAdmins.getOrDefault(groupName, "");
                writer.println(groupName + "|" + admin + "|" + String.join(",", entry.getValue()));
            }
        } catch (IOException e) {
            System.out.println("Error saving groups");
        }
    }

    private void handleSignup(String username, String password) {
        try {
            File file = new File(USER_FILE);
            if (!file.exists()) file.createNewFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 1 && parts[0].equals(username)) {
                        output.println("SIGNUP_FAILED"); return;
                    }
                }
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println(username + "|" + password);
            }
            output.println("SIGNUP_SUCCESS");
            System.out.println("New user registered: " + username);
        } catch (IOException e) {
            output.println("SIGNUP_FAILED");
        }
    }

    private void handleLogin(String username, String password) {
        try {
            for (ClientHandler c : clients) {
                if (c.authenticated && username.equals(c.username)) {
                    output.println("LOGIN_FAILED"); return;
                }
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                        this.username = username;
                        this.authenticated = true;
                        output.println("LOGIN_SUCCESS");
                        System.out.println(username + " logged in.");
                        broadcastSystem(username + " joined the chat");
                        broadcastOnlineList();
                        sendMyGroups();
                        sendHistoryToClient();
                        return;
                    }
                }
            }
            output.println("LOGIN_FAILED");
        } catch (IOException e) {
            output.println("LOGIN_FAILED");
        }
    }

    private void handleChangePassword(String oldPassword, String newPassword) {
        try {
            File file = new File(USER_FILE);
            List<String> lines = new ArrayList<>();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2 && parts[0].equals(username)) {
                        if (parts[1].equals(oldPassword)) {
                            lines.add(username + "|" + newPassword);
                            found = true;
                        } else {
                            output.println("CHANGE_PASS_FAILED|Wrong current password"); return;
                        }
                    } else lines.add(line);
                }
            }
            if (found) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    for (String l : lines) writer.println(l);
                }
                output.println("CHANGE_PASS_SUCCESS");
            } else output.println("CHANGE_PASS_FAILED|User not found");
        } catch (IOException e) {
            output.println("CHANGE_PASS_FAILED|Error");
        }
    }

    private void saveHistory(String key, String line) {
        try {
            File file = new File(HISTORY_DIR + "/" + key + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error saving history");
        }
    }

    private void sendHistoryToClient() {
        sendFileHistory("public", "HISTORY|GROUP|");
        File dir = new File(HISTORY_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("private_") && name.contains(username));
        if (files != null) {
            for (File f : files) {
                String key = f.getName().replace(".txt", "");
                String[] parts = key.replace("private_", "").split("_");
                if (parts.length >= 2) {
                    String otherUser = parts[0].equals(username) ? parts[1] : parts[0];
                    sendFileHistory(key, "HISTORY|PRIVATE|" + otherUser + "|");
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
            if (entry.getValue().contains(username)) {
                String gName = entry.getKey();
                String fileKey = "group_" + gName.replaceAll("[^a-zA-Z0-9]", "_");
                sendFileHistory(fileKey, "HISTORY|GROUPCHAT|" + gName + "|");
            }
        }
        output.println("HISTORY_END");
    }

    private void sendFileHistory(String fileKey, String prefix) {
        File file = new File(HISTORY_DIR + "/" + fileKey + ".txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.println(prefix + line);
            }
        } catch (IOException ignored) {}
    }

    private void handleCreateGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            output.println("SYSTEM|Group name cannot be empty"); return;
        }
        groupName = groupName.trim();
        if (groups.containsKey(groupName)) {
            output.println("SYSTEM|Group already exists"); return;
        }
        Set<String> members = ConcurrentHashMap.newKeySet();
        members.add(username);
        groups.put(groupName, members);
        groupAdmins.put(groupName, username);
        output.println("GROUP_SYSTEM|" + groupName + "|Group \"" + groupName + "\" created successfully");
        sendMyGroups();
        saveGroups();
    }

    private void handleAddToGroup(String groupName, String usersCsv) {
        Set<String> members = groups.get(groupName);
        if (members == null || !members.contains(username)) {
            output.println("SYSTEM|Cannot add members"); return;
        }
        String[] users = usersCsv.split(",");
        List<String> added = new ArrayList<>();
        for (String target : users) {
            target = target.trim();
            if (target.isEmpty() || members.contains(target) || !userExists(target)) continue;
            members.add(target);
            added.add(target);
            for (ClientHandler c : clients) {
                if (c.authenticated && target.equals(c.username)) {
                    c.output.println("GROUP_SYSTEM|" + groupName + "|You were added to this group by " + username);
                    c.sendMyGroups(); break;
                }
            }
        }
        if (!added.isEmpty()) {
            output.println("GROUP_SYSTEM|" + groupName + "|Added: " + String.join(", ", added));
            sendMyGroups();
            saveGroups();
        }
    }

    private void handleRemoveFromGroup(String groupName, String targetUser) {
        Set<String> members = groups.get(groupName);
        String admin = groupAdmins.get(groupName);
        if (members == null || !username.equals(admin) || targetUser.equals(admin) || !members.contains(targetUser)) {
            output.println("SYSTEM|Cannot remove member"); return;
        }
        members.remove(targetUser);
        sendGroupSystem(groupName, "\"" + targetUser + "\" was removed by admin");
        for (ClientHandler c : clients) {
            if (c.authenticated && targetUser.equals(c.username)) {
                c.output.println("SYSTEM|You were removed from group \"" + groupName + "\"");
                c.sendMyGroups(); break;
            }
        }
        sendMyGroups();
        saveGroups();
    }

    private void handleGroupMessage(String groupName, String text) {
        Set<String> members = groups.get(groupName);
        if (members == null || !members.contains(username)) return;
        String fullMessage = "GROUP_MSG|" + groupName + "|" + username + "|" + text;
        for (ClientHandler c : clients) {
            if (c.authenticated && members.contains(c.username)) {
                c.output.println(fullMessage);
            }
        }
        String fileKey = "group_" + groupName.replaceAll("[^a-zA-Z0-9]", "_");
        saveHistory(fileKey, username + ": " + text);
    }

    private void sendGroupSystem(String groupName, String text) {
        Set<String> members = groups.get(groupName);
        if (members == null) return;
        String msg = "GROUP_SYSTEM|" + groupName + "|" + text;
        for (ClientHandler c : clients) {
            if (c.authenticated && members.contains(c.username)) {
                c.output.println(msg);
            }
        }
    }

    private void sendMyGroups() {
        List<String> myGroups = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
            if (entry.getValue().contains(username)) myGroups.add(entry.getKey());
        }
        Collections.sort(myGroups);
        output.println("MY_GROUPS|" + String.join(",", myGroups));
    }

    private void sendGroupMembers(String groupName) {
        Set<String> members = groups.get(groupName);
        if (members == null) { output.println("GROUP_MEMBERS|"); return; }
        List<String> list = new ArrayList<>(members);
        Collections.sort(list);
        String admin = groupAdmins.getOrDefault(groupName, "");
        output.println("GROUP_MEMBERS|" + admin + "|" + String.join(",", list));
    }

    private void sendAllRegisteredUsers() {
        List<String> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 1 && !parts[0].trim().isEmpty()) users.add(parts[0].trim());
            }
        } catch (IOException ignored) {}
        Collections.sort(users);
        output.println("ALL_USERS|" + String.join(",", users));
    }

    private boolean userExists(String user) {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 1 && parts[0].equals(user)) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client.authenticated && client.output != null) client.output.println(message);
        }
    }

    private void broadcastSystem(String text) {
        broadcast("SYSTEM|" + text);
    }

    private void broadcastOnlineList() {
        List<String> online = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.authenticated && c.username != null) online.add(c.username);
        }
        Collections.sort(online);
        broadcast("ONLINE|" + String.join(",", online));
    }

    private void sendPrivate(String toUser, String fromUser, String text) {
        boolean found = false;
        for (ClientHandler client : clients) {
            if (client.authenticated && toUser.equals(client.username)) {
                client.output.println("PRIVATE|" + fromUser + "|" + text);
                found = true; break;
            }
        }
        this.output.println("PRIVATE|" + fromUser + "|" + text);
        String u1 = fromUser.compareTo(toUser) < 0 ? fromUser : toUser;
        String u2 = fromUser.compareTo(toUser) < 0 ? toUser : fromUser;
        saveHistory("private_" + u1 + "_" + u2, fromUser + ": " + text);
        if (!found) this.output.println("SYSTEM|User \"" + toUser + "\" is currently offline.");
    }
}