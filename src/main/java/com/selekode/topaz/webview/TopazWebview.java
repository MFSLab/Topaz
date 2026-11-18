package com.selekode.topaz.webview;

import com.selekode.topaz.bootstrap.AppDirectories;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TopazWebview {

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "false");
		System.out.println("PROCESS: Initializing Chromium (JCEF)");

		String activePort = System.getProperty("app.active.port", "8080");
		String targetUrl = "http://localhost:" + activePort + "/login";

		// 1. Resolve OS-specific AppData directory for Topaz
		File installDir = new File(AppDirectories.getJcefBundleDirectory());
		File cacheDir = new File(AppDirectories.getJcefCacheDirectory());
		installDir.mkdirs();
		cacheDir.mkdirs();

		BufferedImage appIcon = loadAppIcon();

		// 2. Set up Splash Screen (only shown if JCEF needs to download/extract)
		SplashWindow splash = new SplashWindow(appIcon);

		CefAppBuilder builder = new CefAppBuilder();
		builder.setInstallDir(installDir);
		builder.getCefSettings().root_cache_path = cacheDir.getAbsolutePath();
		builder.getCefSettings().windowless_rendering_enabled = false;

		builder.setProgressHandler(new IProgressHandler() {
			@Override
			public void handleProgress(EnumProgress state, float percent) {
				if (state == EnumProgress.DOWNLOADING || state == EnumProgress.EXTRACTING) {
					splash.showIfNeeded();
					int pct = Math.max(0, Math.min(100, Math.round(percent)));
					String label = (state == EnumProgress.DOWNLOADING)
							? "Descargando librerías adicionales... " + pct + "%"
							: "Extrayendo librerías adicionales... " + pct + "%";
					splash.updateProgress(label, pct, false);
				} else if (state == EnumProgress.INSTALL || state == EnumProgress.INITIALIZING) {
					if (splash.isVisible()) {
						splash.updateProgress("Cargando Topaz...", 100, true);
					}
				} else if (state == EnumProgress.INITIALIZED) {
					splash.close();
				}
			}
		});

		builder.setAppHandler(new MavenCefAppHandlerAdapter() {
			@Override
			public void stateHasChanged(CefApp.CefAppState state) {
				if (state == CefApp.CefAppState.TERMINATED) {
					System.exit(0);
				}
			}
		});

		CefApp cefApp = builder.build();
		splash.close(); // Ensure splash is closed once build finishes

		CefClient client = cefApp.createClient();
		CefBrowser browser = client.createBrowser(targetUrl, false, false);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Topaz");
			frame.setSize(1280, 720);
			frame.setMinimumSize(new Dimension(1010, 600));
			frame.setLocationRelativeTo(null);

			if (appIcon != null) {
				frame.setIconImage(appIcon);
			}

			frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.out.println("PROCESS: Closing Topaz...");
					CefApp.getInstance().dispose();
					System.exit(0);
				}
			});

			frame.setVisible(true);
			System.out.println("PROCESS: Opened Chromium WebView on " + targetUrl);
		});
	}

	private static BufferedImage loadAppIcon() {
		try (InputStream is = TopazWebview.class.getResourceAsStream("/static/images/topaz-logo.png")) {
			if (is != null) {
				return ImageIO.read(is);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * Lightweight Swing splash window shown only during first-run setup.
	 */
	private static class SplashWindow {
		private final JDialog dialog;
		private final JLabel statusLabel;
		private final JProgressBar progressBar;
		private volatile boolean shown = false;

		SplashWindow(BufferedImage icon) {
			dialog = new JDialog((Frame) null, "Topaz Setup", false);
			dialog.setUndecorated(true);
			if (icon != null) {
				dialog.setIconImage(icon);
			}

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
					new EmptyBorder(24, 32, 24, 32)
			));
			panel.setBackground(new Color(28, 28, 32));

			JLabel titleLabel = new JLabel("Topaz");
			titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
			titleLabel.setForeground(Color.WHITE);
			titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

			statusLabel = new JLabel("Preparing workspace...");
			statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
			statusLabel.setForeground(new Color(190, 190, 200));
			statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

			progressBar = new JProgressBar(0, 100);
			progressBar.setPreferredSize(new Dimension(320, 10));
			progressBar.setMaximumSize(new Dimension(320, 10));
			progressBar.setStringPainted(false);
			progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

			panel.add(titleLabel);
			panel.add(Box.createVerticalStrut(12));
			panel.add(statusLabel);
			panel.add(Box.createVerticalStrut(14));
			panel.add(progressBar);

			dialog.getContentPane().add(panel);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
		}

		void showIfNeeded() {
			if (!shown) {
				shown = true;
				SwingUtilities.invokeLater(() -> dialog.setVisible(true));
			}
		}

		boolean isVisible() {
			return shown;
		}

		void updateProgress(String text, int percent, boolean indeterminate) {
			SwingUtilities.invokeLater(() -> {
				statusLabel.setText(text);
				progressBar.setIndeterminate(indeterminate);
				if (!indeterminate) {
					progressBar.setValue(percent);
				}
			});
		}

		void close() {
			if (shown) {
				shown = false;
				SwingUtilities.invokeLater(dialog::dispose);
			}
		}
	}
}