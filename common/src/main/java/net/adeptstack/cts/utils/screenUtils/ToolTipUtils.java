package net.adeptstack.cts.utils.screenUtils;

public class ToolTipUtils {
    public static String GetNLPlatformBlockToolTipName(int variant) {
        int signBlockCount = 0;
        String name = "";

        for (int i = 0; i < variant; i++) {
            if (i % 5 == 0) {
                signBlockCount++;
            }
        }

        if (variant == 0) {
            name = "0";
        } else if (variant % 5 == 1) {
            name = signBlockCount + "";
        } else if (variant % 5 == 2) {
            name = signBlockCount + "A";
        } else if (variant % 5 == 3) {
            name = signBlockCount + "B";
        } else if (variant % 5 == 4) {
            name = signBlockCount + "C";
        } else if (variant % 5 == 0) {
            name = signBlockCount + "D";
        }
        return name;
    }

    public static String GetDEPlatformBlockToolTipName(int variant) {
        String name = switch (variant) {
            case 0 -> "0";
            case 17 -> "A";
            case 18 -> "B";
            case 19 -> "C";
            case 20 -> "D";
            case 21 -> "E";
            case 22 -> "F";
            case 23 -> "-";
            default -> "" + variant;
        };
        return name;
    }

    public static String GetCHPlatformBlockToolTipName(int variant) {
        String name = switch (variant) {
            case 0 -> "0";
            case 17 -> "A";
            case 18 -> "B";
            case 19 -> "C";
            case 20 -> "D";
            case 21 -> "E";
            case 22 -> "F";
            default -> variant + "";
        };
        return name;
    }
}
