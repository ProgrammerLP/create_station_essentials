package net.adeptstack.cts.utils.screenUtils;

public class TextureNames {
    public static String GetNLPlatformBlockTextureName(int variant) {
        int signBlockCount = 0;
        String name = "";

        for (int i = 0; i < variant; i++) {
            if (i % 5 == 0) {
                signBlockCount++;
            }
        }

        if (variant == 0) {
            name = "empty_platform_block.png";
        } else if (variant % 5 == 1) {
            name = "platform" + signBlockCount + "_block.png";
        } else if (variant % 5 == 2) {
            name = "platform" + signBlockCount + "a_block.png";
        } else if (variant % 5 == 3) {
            name = "platform" + signBlockCount + "b_block.png";
        } else if (variant % 5 == 4) {
            name = "platform" + signBlockCount + "c_block.png";
        } else if (variant % 5 == 0) {
            name = "platform" + signBlockCount + "d_block.png";
        }
        return name;
    }

    public static String GetDEPlatformBlockTextureName(int variant) {
        String name = switch (variant) {
            case 0 -> "empty_platform_block.png";
            case 17 -> "platform_a_block.png";
            case 18 -> "platform_b_block.png";
            case 19 -> "platform_c_block.png";
            case 20 -> "platform_d_block.png";
            case 21 -> "platform_e_block.png";
            case 22 -> "platform_f_block.png";
            case 23 -> "platform_to_block.png";
            default -> "left/platform" + variant + "_block.png";
        };
        return name;
    }

    public static String GetCHPlatformBlockTextureName(int variant) {
        String name = switch (variant) {
            case 0 -> "empty.png";
            case 17 -> "a.png";
            case 18 -> "b.png";
            case 19 -> "c.png";
            case 20 -> "d.png";
            case 21 -> "e.png";
            case 22 -> "f.png";
            default -> variant + ".png";
        };
        return name;
    }
}
