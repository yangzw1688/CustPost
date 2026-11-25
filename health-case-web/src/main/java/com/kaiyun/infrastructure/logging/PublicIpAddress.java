package com.kaiyun.infrastructure.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** 只解析 IP 字面量，过滤本机、局域网及常见保留地址，不进行 DNS 查询。 */
public final class PublicIpAddress {
    private PublicIpAddress() { }

    public static String parse(String input) {
        if (input == null || input.length() > 64) return null;
        String value = input.trim();
        byte[] bytes;
        try {
            if (value.indexOf(':') >= 0 && value.matches("[0-9a-fA-F:.]+")) {
                bytes = InetAddress.getByName(value).getAddress();
            } else {
                if (!value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) return null;
                String[] parts = value.split("\\."); bytes = new byte[4];
                for (int index = 0; index < 4; index++) {
                    if (parts[index].length() > 3) return null;
                    int part = Integer.parseInt(parts[index]);
                    if (part > 255) return null;
                    bytes[index] = (byte) part;
                }
            }
            InetAddress address = InetAddress.getByAddress(bytes);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) return null;
            int first = bytes[0] & 255, second = bytes[1] & 255;
            if (bytes.length == 4) {
                int third = bytes[2] & 255;
                if (first == 0 || first >= 224 || first == 100 && second >= 64 && second <= 127
                        || first == 192 && second == 0 && (third == 0 || third == 2)
                        || first == 192 && second == 88 && third == 99
                        || first == 198 && (second == 18 || second == 19 || second == 51 && third == 100)
                        || first == 203 && second == 0 && third == 113) return null;
            } else {
                if ((first & 0xe0) != 0x20 || first == 0x20 && second == 1 && (bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8) return null;
            }
            return address.getHostAddress();
        } catch (UnknownHostException | NumberFormatException error) { return null; }
    }
}
