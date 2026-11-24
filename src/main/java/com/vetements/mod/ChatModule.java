package com.vetements.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class ChatModule {
    private static final int MAX_CHAT_LENGTH = 256;
    private static final int LOGIN_WINDOW_TICKS = 20 * 60;
    private static final String[] MATH_KEYWORDS = {
            "реши", "пример", "сколько", "вычисл", "посчита", "решить",
            "solve", "calc", "math", "answer", "=?", "= ?"
    };

    private static final List<Pending> PENDING = new ArrayList<>();
    private static ClientPlayerEntity lastPlayer;
    private static boolean loginHandled;
    private static int joinTicks;
    private static String lastExpression = "";
    private static long lastSolveTime;

    private ChatModule() {
    }

    private static final class Pending {
        int ticks;
        final String text;

        Pending(int ticks, String text) {
            this.ticks = ticks;
            this.text = text;
        }
    }

    // ------------------------------------------------------------ outgoing

    /** Adds style, prefix and suffix to a normal chat message. Commands are left untouched. */
    public static String onOutgoing(String message) {
        Config cfg = Config.get();
        if (message == null || !cfg.chatEnabled) {
            return message;
        }
        if (message.startsWith("/") || message.startsWith(".")) {
            return message;
        }
        String body = ChatStyle.fromId(cfg.style).apply(message);
        StringBuilder sb = new StringBuilder();
        if (!cfg.prefix.isEmpty()) {
            sb.append(cfg.prefix).append(' ');
        }
        sb.append(body);
        if (!cfg.suffix.isEmpty()) {
            sb.append(' ').append(cfg.suffix);
        }
        String result = sb.toString();
        return result.length() > MAX_CHAT_LENGTH ? message : result;
    }

    /** Handles ".vet ..." settings commands. Returns true if the message was a command. */
    public static boolean handleCommand(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.equals(".vet") && !lower.startsWith(".vet ")) {
            return false;
        }
        Config cfg = Config.get();
        String rest = message.length() > 4 ? message.substring(4).trim() : "";
        String[] parts = rest.split("\\s+", 2);
        String sub = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (sub) {
            case "chat":
                cfg.chatEnabled = parseOnOff(arg, cfg.chatEnabled);
                info("Оформление чата: " + onOff(cfg.chatEnabled));
                break;
            case "prefix":
                cfg.prefix = isOff(arg) ? "" : arg;
                info("Префикс: " + (cfg.prefix.isEmpty() ? "выключен" : cfg.prefix));
                break;
            case "suffix":
                cfg.suffix = isOff(arg) ? "" : arg;
                info("Суффикс: " + (cfg.suffix.isEmpty() ? "выключен" : cfg.suffix));
                break;
            case "style":
                if (ChatStyle.exists(arg)) {
                    cfg.style = arg.toLowerCase(Locale.ROOT);
                    info("Стиль: " + cfg.style);
                } else {
                    info("§cТакого стиля нет. Доступные:");
                    for (ChatStyle s : ChatStyle.values()) {
                        info("§7 " + s.id + " - " + s.description);
                    }
                }
                break;
            case "math":
                cfg.mathSolver = parseOnOff(arg, cfg.mathSolver);
                info("Авторешение примеров: " + onOff(cfg.mathSolver));
                break;
            case "authme":
                cfg.authmePassword = isOff(arg) ? "" : arg;
                info("Автовход AuthMe: " + (cfg.authmePassword.isEmpty() ? "выключен" : "включён"));
                break;
            case "status":
                info("§6Vetements: §fчат " + onOff(cfg.chatEnabled)
                        + "§f, стиль " + cfg.style
                        + ", префикс [" + cfg.prefix + "], суффикс [" + cfg.suffix + "]"
                        + ", примеры " + onOff(cfg.mathSolver)
                        + ", AuthMe " + (cfg.authmePassword.isEmpty() ? "выкл" : "вкл"));
                break;
            default:
                help();
                return true;
        }
        Config.save();
        return true;
    }

    private static void help() {
        info("§6Vetements - команды (пишутся в чат):");
        info("§e.vet chat on/off §7- оформление чата");
        info("§e.vet prefix <текст> §7(или off) - префикс перед сообщением");
        info("§e.vet suffix <текст> §7(или off) - суффикс после сообщения");
        info("§e.vet style <plain|smallcaps|fullwidth|bold> §7- стиль текста");
        info("§e.vet math on/off §7- авторешение примеров");
        info("§e.vet authme <пароль> §7(или off) - автовход");
        info("§e.vet status §7- текущие настройки");
    }

    private static boolean isOff(String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        return a.isEmpty() || a.equals("off") || a.equals("выкл");
    }

    private static boolean parseOnOff(String arg, boolean current) {
        String a = arg.toLowerCase(Locale.ROOT);
        if (a.equals("on") || a.equals("вкл") || a.equals("1")) return true;
        if (a.equals("off") || a.equals("выкл") || a.equals("0")) return false;
        return !current;
    }

    private static String onOff(boolean value) {
        return value ? "§aвкл" : "§cвыкл";
    }

    private static void info(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(new LiteralText(text));
        }
    }

    // ------------------------------------------------------------ incoming

    public static void onIncoming(String raw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || raw == null) {
            return;
        }
        Config cfg = Config.get();
        String stripped = Formatting.strip(raw);
        String text = stripped == null ? raw : stripped;
        String lower = text.toLowerCase(Locale.ROOT);

        // AuthMe auto login: only shortly after joining a server, and only once.
        if (!cfg.authmePassword.isEmpty() && !loginHandled
                && joinTicks < LOGIN_WINDOW_TICKS && lower.contains("/login")) {
            loginHandled = true;
            queue(20, "/login " + cfg.authmePassword);
            return;
        }

        if (cfg.mathSolver && containsKeyword(lower)) {
            String answer = MathSolver.solve(text);
            if (answer != null) {
                long now = System.currentTimeMillis();
                if (answer.equals(lastExpression) && now - lastSolveTime < 5000) {
                    return;
                }
                lastExpression = answer;
                lastSolveTime = now;
                queue(Math.max(1, cfg.mathDelayTicks), answer);
            }
        }
    }

    private static boolean containsKeyword(String lower) {
        for (String k : MATH_KEYWORDS) {
            if (lower.contains(k)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ ticking

    private static void queue(int ticks, String text) {
        PENDING.add(new Pending(ticks, text));
    }

    public static void tick(MinecraftClient client) {
        if (client.player != lastPlayer) {
            lastPlayer = client.player;
            loginHandled = false;
            joinTicks = 0;
            PENDING.clear();
        }
        if (client.player == null) {
            return;
        }
        joinTicks++;
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            p.ticks--;
            if (p.ticks <= 0) {
                it.remove();
                sendRaw(client, p.text);
            }
        }
    }

    /** Sends text exactly as is (no prefix, suffix or style). */
    private static void sendRaw(MinecraftClient client, String text) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            handler.sendPacket(new ChatMessageC2SPacket(text));
        }
    }
}
