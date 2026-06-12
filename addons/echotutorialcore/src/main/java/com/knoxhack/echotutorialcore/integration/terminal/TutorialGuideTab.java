package com.knoxhack.echotutorialcore.integration.terminal;

import com.echoplatform.echocore.api.EchoOptionalServices;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.client.TutorialClientData;
import com.knoxhack.echotutorialcore.client.TutorialClientDisplay;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class TutorialGuideTab implements ClientTerminalTab {
    private static final int ACCENT = 0xFF92F7A6;
    private static final int ROW_H = 15;
    private final TerminalTabDescriptor descriptor =
            new TerminalTabDescriptor(TutorialTerminalClientIntegration.TAB_ID, "GUIDE", 45, ACCENT);
    private final TerminalTabChrome chrome =
            TerminalTabChrome.of("ECHO Guide", TerminalTabChrome.GROUP_FIELD, "GD",
                    "Tutorial cards, hints, and guidance", 45);

    private Identifier selectedCardId = null;
    private String searchText = "";

    @Override
    public TerminalTabDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public TerminalTabChrome chrome() {
        return chrome;
    }

    @Override
    public void onSelected(TerminalRenderContext context) {
        if (selectedCardId == null && !cards().isEmpty()) {
            selectedCardId = cards().get(0).id();
        }
    }

    @Override
    public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick) {
        int x = context.contentX() + 12;
        int y = context.contentY() + 10 - context.scrollY();
        int w = context.contentWidth() - 24;
        int accent = accent();

        y = TerminalUi.sectionHeader(context, graphics, "ECHO GUIDE", "TUTORIALCORE", x, y, w, accent);
        y += 5;
        y = renderControls(context, graphics, x, y, w);
        y += 8;

        List<CardView> cards = cards();
        if (cards.isEmpty()) {
            TerminalUi.flatHudPanel(context, graphics, x, y, w, 54, accent);
            TerminalUi.wrap(context, graphics, "No synced guide cards yet. Reopen the Terminal after login or run /reload.",
                    x + 12, y + 14, w - 24, TerminalUi.muted(context));
            return;
        }

        for (TutorialCategory category : TutorialCategory.values()) {
            List<CardView> sectionCards = cards.stream()
                    .filter(card -> card.category().equals(category.name()))
                    .toList();
            if (sectionCards.isEmpty()) continue;

            int sectionH = estimateSectionHeight(sectionCards, selectedCardId);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, sectionH, accent);
            int sy = y + 8;
            TerminalUi.line(context, graphics, category.name().replace('_', ' '), x + 12, sy, w - 24, accent);
            sy += 16;

            for (CardView card : sectionCards) {
                boolean selected = card.id().equals(selectedCardId);
                boolean unread = TutorialClientData.isUnread(card.id());
                graphics.fill(x + 8, sy, x + w - 8, sy + ROW_H - 1,
                        selected ? TerminalUi.ROW_SELECTED : TerminalUi.ROW);
                TerminalUi.line(context, graphics, unread ? "* " + card.title() : card.title(),
                        x + 14, sy + 3, w - 28, selected ? accent : TerminalUi.text(context));
                sy += ROW_H;

                if (selected) {
                    sy = renderCardDetail(context, graphics, card, x + 14, sy + 3, w - 28);
                }
            }
            y += sectionH + 10;
        }
    }

    @Override
    public int contentHeight(TerminalRenderContext context) {
        int h = 108;
        for (TutorialCategory category : TutorialCategory.values()) {
            List<CardView> sectionCards = cards().stream()
                    .filter(card -> card.category().equals(category.name()))
                    .toList();
            if (!sectionCards.isEmpty()) {
                h += estimateSectionHeight(sectionCards, selectedCardId) + 10;
            }
        }
        return h + 20;
    }

    @Override
    public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int x = context.contentX() + 12;
        int y = context.contentY() + 10 - context.scrollY();
        int w = context.contentWidth() - 24;
        y += 50;
        String[] modes = {"OFF", "MINIMAL", "NORMAL", "ASSISTED"};
        int bx = x + 92;
        for (String mode : modes) {
            int bw = Math.max(48, mode.length() * 7 + 14);
            if (TerminalUi.inside(mouseX, mouseY, bx, y + 8, bw, 16)) {
                TutorialClientDisplay.requestGuideMode(mode);
                context.playCommandSound();
                return true;
            }
            bx += bw + 5;
        }
        y += 54;

        for (TutorialCategory category : TutorialCategory.values()) {
            List<CardView> sectionCards = cards().stream()
                    .filter(card -> card.category().equals(category.name()))
                    .toList();
            if (sectionCards.isEmpty()) continue;
            int sectionH = estimateSectionHeight(sectionCards, selectedCardId);
            int sy = y + 24;
            for (CardView card : sectionCards) {
                if (TerminalUi.inside(mouseX, mouseY, x + 8, sy, w - 16, ROW_H)) {
                    selectedCardId = selectedCardId != null && selectedCardId.equals(card.id()) ? null : card.id();
                    context.playCommandSound();
                    return true;
                }
                sy += ROW_H;
                if (card.id().equals(selectedCardId)) {
                    sy += detailHeight(card);
                }
            }
            y += sectionH + 10;
        }
        return false;
    }

    @Override
    public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.offsetByCodePoints(searchText.length(), -1));
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !searchText.isEmpty()) {
            searchText = "";
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
        if (event == null || !event.isAllowedChatCharacter() || searchText.length() >= 36) {
            return false;
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isBlank()) {
            return false;
        }
        searchText += typed.toLowerCase(Locale.ROOT);
        return true;
    }

    private int renderControls(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w) {
        int accent = accent();
        TerminalUi.flatHudPanel(context, graphics, x, y, w, 54, accent);
        TerminalUi.line(context, graphics, "Guide Mode", x + 12, y + 11, 76, TerminalUi.muted(context));
        String[] modes = {"OFF", "MINIMAL", "NORMAL", "ASSISTED"};
        int bx = x + 92;
        for (String mode : modes) {
            int bw = Math.max(48, mode.length() * 7 + 14);
            boolean selected = mode.equalsIgnoreCase(TutorialClientData.guideMode());
            graphics.fill(bx, y + 8, bx + bw, y + 24, selected ? 0x4439D882 : TerminalUi.ROW);
            TerminalUi.line(context, graphics, mode, bx + 7, y + 12, bw - 14, selected ? accent : TerminalUi.text(context));
            bx += bw + 5;
        }
        String search = searchText.isBlank() ? "type to search cards" : searchText;
        TerminalUi.line(context, graphics, "Search", x + 12, y + 33, 76, TerminalUi.muted(context));
        TerminalUi.line(context, graphics, search, x + 92, y + 33, w - 108, searchText.isBlank() ? TerminalUi.muted(context) : TerminalUi.text(context));
        return y + 54;
    }

    private static int accent() {
        return EchoOptionalServices.themeCoreOrNoOp().resolveColor("accent.primary", ACCENT);
    }

    private List<CardView> cards() {
        List<CardView> source = TutorialClientData.hasContent()
                ? TutorialClientData.cards().stream().map(CardView::fromClient).toList()
                : TutorialCoreRegistries.allCards().stream().map(CardView::fromServer).toList();
        if (searchText.isBlank()) {
            return source;
        }
        String needle = searchText.toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(card -> card.searchText().contains(needle))
                .toList();
    }

    private static int estimateSectionHeight(List<CardView> cards, Identifier selectedCardId) {
        int h = 28;
        for (CardView card : cards) {
            h += ROW_H;
            if (card.id().equals(selectedCardId)) {
                h += detailHeight(card);
            }
        }
        return Math.max(h, 44);
    }

    private int renderCardDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            CardView card, int x, int y, int maxW) {
        int startY = y;
        if (!card.summary().isBlank()) {
            y = TerminalUi.wrap(context, graphics, card.summary(), x, y, maxW, TerminalUi.muted(context));
            y += 4;
        }
        for (String paragraph : card.body()) {
            y = TerminalUi.wrap(context, graphics, paragraph, x, y, maxW, TerminalUi.muted(context));
            y += 4;
        }
        if (!card.steps().isEmpty()) {
            y = TerminalUi.wrap(context, graphics, "Steps", x, y, maxW, ACCENT);
            y += 2;
            for (String step : card.steps()) {
                y = TerminalUi.wrap(context, graphics, "- " + step, x, y, maxW, TerminalUi.text(context));
                y += 2;
            }
        }
        if (!card.related().isEmpty()) {
            y = TerminalUi.wrap(context, graphics, "Related: " + String.join(", ", card.related()),
                    x, y, maxW, TerminalUi.muted(context));
            y += 4;
        }
        return y + 8 - startY + startY;
    }

    private static int detailHeight(CardView card) {
        int h = card.summary().isBlank() ? 0 : 16;
        h += card.body().size() * 18;
        h += card.steps().isEmpty() ? 0 : 16 + card.steps().size() * 16;
        h += card.related().isEmpty() ? 0 : 18;
        return h + 12;
    }

    private record CardView(
            Identifier id,
            String category,
            String title,
            String summary,
            List<String> body,
            List<String> steps,
            List<String> related,
            String searchText) {
        static CardView fromClient(com.knoxhack.echotutorialcore.network.SyncTutorialContentPacket.CardData card) {
            return new CardView(card.id(), card.category(), card.title(), card.summary(), card.body(), card.steps(),
                    card.related().stream().map(Identifier::toString).toList(), search(card.title(), card.summary(), card.body(), card.steps()));
        }

        static CardView fromServer(com.knoxhack.echotutorialcore.api.card.TutorialCard card) {
            return new CardView(card.id(), card.category().name(), card.title(), card.summary(), card.body(), card.steps(),
                    card.related().stream().map(Identifier::toString).toList(), search(card.title(), card.summary(), card.body(), card.steps()));
        }
        private static String search(String title, String summary, List<String> body, List<String> steps) {
            return (title + " " + summary + " " + String.join(" ", body) + " " + String.join(" ", steps))
                    .toLowerCase(Locale.ROOT);
        }
    }
}
