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
import java.awt.geom.RoundRectangle2D;
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
		private static Font ubuntuFont = null;

		// Static block to load the Ubuntu font (Make sure "Ubuntu-B.ttf" is in your project resources)
		static {
			try (InputStream is = SplashWindow.class.getResourceAsStream("/fonts/Ubuntu-B.ttf")) {
				if (is != null) {
					ubuntuFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.BOLD, 22f);
					GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(ubuntuFont);
				}
			} catch (Exception e) {
				// Fallback to SansSerif if font file is missing
				ubuntuFont = new Font("SansSerif", Font.BOLD, 22);
			}
		}

		SplashWindow(BufferedImage icon) {
			// macOS-friendly undecorated dialog
			dialog = new JDialog((Frame) null, "Topaz Setup", false);
			dialog.setUndecorated(true);
			if (icon != null) {
				dialog.setIconImage(icon);
			}

			// Enable window translucency/shape rounding for macOS native card look
			dialog.setBackground(new Color(0, 0, 0, 0));

			JPanel panel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					// macOS Dark Mode background color (slightly translucent or rich dark gray)
					g2.setColor(new Color(40, 40, 45, 245));
					// Draw rounded rectangle with a 12px corner radius (native macOS window corner style)
					g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));

					// Subtle thin native border stroke
					g2.setColor(new Color(255, 255, 255, 30));
					g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

					g2.dispose();
				}
			};

			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			// Generous macOS-style padding
			panel.setBorder(new EmptyBorder(28, 36, 28, 36));
			panel.setOpaque(false);

			JLabel titleLabel = new JLabel("Topaz");
			// Apply Ubuntu font if loaded successfully
			titleLabel.setFont(ubuntuFont != null ? ubuntuFont : new Font("SansSerif", Font.BOLD, 22));
			titleLabel.setForeground(new Color(245, 245, 247)); // Apple's high-legibility system white
			titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

			statusLabel = new JLabel("Preparing workspace...");
			statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			statusLabel.setForeground(new Color(152, 152, 157)); // Apple secondary label color
			statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

			progressBar = new JProgressBar(0, 100);
			progressBar.setPreferredSize(new Dimension(280, 6)); // Thinner, sleeker macOS style track
			progressBar.setMaximumSize(new Dimension(280, 6));
			progressBar.setStringPainted(false);
			progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

			panel.add(titleLabel);
			panel.add(Box.createVerticalStrut(8));
			panel.add(statusLabel);
			panel.add(Box.createVerticalStrut(18));
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