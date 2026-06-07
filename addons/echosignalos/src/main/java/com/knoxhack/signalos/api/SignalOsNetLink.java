package com.knoxhack.signalos.api;

/**
 * Curated SignalNet link. Links target only other SignalNet addresses, never
 * external HTTP URLs.
 */
public record SignalOsNetLink(String label, String address) {
    public SignalOsNetLink {
        label = label == null || label.isBlank() ? "Link" : label.strip();
        address = SignalOsNetSite.cleanAddress(address);
    }
}
