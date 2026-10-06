#!/usr/bin/env python3
"""
Automated Localization Synchronization & Structuring Tool
Maintains 1:1 section structure, comments, and key parity between base English strings.xml
and all localized strings.xml files (values-*/strings.xml).
"""

import os
import re
import xml.etree.ElementTree as ET

BASE_FILE = 'app/src/main/res/values/strings.xml'
RES_DIR = 'app/src/main/res'

def escape_xml(text):
    if not text:
        return ""
    # Escape & that is not already an entity
    text = re.sub(r'&(?!(?:amp|lt|gt|quot|apos);)', '&amp;', text)
    return text

# High-quality idiomatic translations for new strings across languages
NEW_TRANSLATIONS = {
    'de': {
        'btn_grant': 'Gewähren',
        'btn_configure': 'Konfigurieren',
        'btn_set_default': 'Als Standard festlegen',
        'status_granted': 'Gewährt',
        'status_enabled': 'Aktiviert',
        'settings_lockscreen_calling_title': 'Vollbild-Anrufe auf dem Sperrbildschirm',
        'settings_lockscreen_calling_sub': 'Eingehende Anrufe sofort im Vollbildmodus anzeigen, wenn das Gerät gesperrt oder der Bildschirm aus ist',
        'settings_full_screen_intent_warning': 'Vollbild-Anrufbenachrichtigungen sind vom System eingeschränkt',
        'settings_overlay_permission_warning': 'Berechtigung über anderen Apps erforderlich für Dynamic Island',
        'settings_all_call_permissions_good': 'Alle Anruf- und Sperrbildschirmfunktionen sind vollständig konfiguriert',
        'settings_recordings_always_on': 'Anrufe immer aufzeichnen',
        'settings_recordings_always_on_sub': 'Startet die Aufzeichnung jedes ein- und ausgehenden Anrufs automatisch bei Verbindungsaufbau',
        'settings_recordings_biometric_lock': 'Aufnahmen mit Biometrie sperren',
        'settings_recordings_biometric_lock_sub': 'Fingerabdruck oder Gerätesperre zum Anhören und Verwalten vertraulicher Aufnahmen anfordern',
        'settings_recordings_auto_export': 'Automatisch in Downloads exportieren',
        'settings_recordings_auto_export_sub': 'Kopiert jede fertige Aufnahme automatisch nach Downloads/SecureDialer für direkten Dateimanager-Zugriff',
        'settings_recordings_compression_title': 'Audiokompression &amp; Qualität',
        'settings_recordings_compression_sub': 'Optimiert Codec-Bitrate und Abtastrate für beste Balance zwischen Audioqualität und Speicherplatz',
        'settings_recordings_cleanup_empty': 'Leere/stille Aufnahmen bereinigen',
        'vault_locked_title': 'Aufnahmetresor gesperrt',
        'vault_locked_desc': 'Biometrische Authentifizierung erforderlich, um auf vertrauliche Anrufaufzeichnungen zuzugreifen.',
        'btn_unlock_vault': 'Tresor entsperren',
        'settings_dynamic_island_title': 'Dynamic Island Anruf-Kapsel',
        'settings_dynamic_island_sub': 'Schwebende interaktive Anrufpille um die Frontkamera für laufende Telefonate anzeigen',
        'settings_dynamic_island_speaker_only': 'Nur im Freisprechmodus anzeigen',
        'settings_dynamic_island_speaker_only_sub': 'Dynamic Island ausschließlich bei aktivem Lautsprechermodus einblenden',
        'settings_dynamic_island_floating_permission': 'Berechtigung für schwebendes Overlay',
        'settings_dynamic_island_floating_permission_sub': 'Schwebende Kapsel über anderen Apps erlauben (Unterstützung für OnePlus, Xiaomi, Vivo, Samsung)',
        'dynamic_island_speaker_on': 'LAUTSPRECHER',
        'dynamic_island_muted': 'Stumm',
        'dynamic_island_recording': 'AUFN',
        'dynamic_island_tap_to_expand': 'Tippen für Anrufsteuerung',
        'callback_reminders_title': 'Rückruf-Erinnerungen',
        'callback_reminders_stats': '%1$d aktiv, %2$d erledigt',
        'header_caller_id_privacy': 'Ausgehende Anrufer-ID &amp; Privatsphäre (CLIR)',
        'settings_hide_caller_id': 'Anrufer-ID verbergen',
        'settings_hide_caller_id_sub': 'Netzbetreiber-Code voranstellen, um Ihre Rufnummer bei ausgehenden Anrufen zu unterdrücken',
        'settings_clir_prefix': 'Netzbetreiber-Präfix',
        'settings_clir_prefix_sub': 'Aktiver Code: %1$s',
        'settings_clir_warning_title': 'Hinweis zu Netzbetreibern &amp; Vorschriften',
        'settings_clir_warning_desc': 'CLIR-Unterstützung variiert je nach Netzanbieter und Region. Notrufnummern werden niemals unterdrückt.',
        'settings_system_sim_caller_id': 'System-SIM-Einstellungen',
        'settings_system_sim_caller_id_sub': 'Öffnet die Android-Systemeinstellungen für Rufnummernanzeige',
        'settings_clir_prefix_dialog_title': 'CLIR-Präfix auswählen',
        'settings_clir_prefix_gsm': 'GSM-Standard (#31#)',
        'settings_clir_prefix_us': 'Nordamerika (*67)',
        'settings_clir_prefix_uk': 'Großbritannien (141)',
        'settings_clir_prefix_japan': 'Japan (1831)',
        'settings_clir_prefix_custom': 'Benutzerdefiniertes Präfix',
        'settings_clir_custom_label': 'Geben Sie den CLIR-Präfixcode Ihres Netzanbieters ein',
        'settings_pure_black_sub_disabled_dark': 'Echtes #000000 Schwarz für maximale OLED-Energieeinsparung (aktiviert Dunkles Design)'
    },
    'es': {
        'btn_grant': 'Conceder',
        'btn_configure': 'Configurar',
        'btn_set_default': 'Establecer predeterminado',
        'status_granted': 'Concedido',
        'status_enabled': 'Activado',
        'settings_lockscreen_calling_title': 'Llamadas a pantalla completa en pantalla de bloqueo',
        'settings_lockscreen_calling_sub': 'Muestra la interfaz de llamada entrante en pantalla completa cuando el dispositivo esté bloqueado o con pantalla apagada',
        'settings_full_screen_intent_warning': 'Las alertas de llamada a pantalla completa están restringidas por el sistema',
        'settings_overlay_permission_warning': 'Permiso de superposición necesario para mostrar Dynamic Island sobre otras apps',
        'settings_all_call_permissions_good': 'Todas las funciones de llamada y pantalla de bloqueo están completamente configuradas',
        'settings_recordings_always_on': 'Grabar llamadas siempre',
        'settings_recordings_always_on_sub': 'Inicia automáticamente la grabación de cada llamada entrante y saliente al conectar',
        'settings_recordings_biometric_lock': 'Bloquear grabaciones con biometría',
        'settings_recordings_biometric_lock_sub': 'Requiere huella dactilar o bloqueo del dispositivo para escuchar grabaciones confidenciales',
        'settings_recordings_auto_export': 'Exportar automáticamente a Descargas',
        'settings_recordings_auto_export_sub': 'Copia cada grabación finalizada a Descargas/SecureDialer para acceso inmediato en el gestor de archivos',
        'settings_recordings_compression_title': 'Compresión y calidad de audio',
        'settings_recordings_compression_sub': 'Optimiza la tasa de bits y muestreo del códec para equilibrar fidelidad y almacenamiento',
        'settings_recordings_cleanup_empty': 'Limpiar grabaciones vacías/silenciosas',
        'vault_locked_title': 'Bóveda de grabaciones bloqueada',
        'vault_locked_desc': 'Se requiere autenticación biométrica para acceder a las grabaciones confidenciales.',
        'btn_unlock_vault': 'Desbloquear bóveda',
        'settings_dynamic_island_title': 'Cápsula de llamadas Dynamic Island',
        'settings_dynamic_island_sub': 'Muestra una píldora interactiva flotante alrededor del recorte de cámara para llamadas en curso',
        'settings_dynamic_island_speaker_only': 'Mostrar solo en altavoz',
        'settings_dynamic_island_speaker_only_sub': 'Muestra Dynamic Island exclusivamente cuando el modo altavoz esté activo',
        'settings_dynamic_island_floating_permission': 'Permiso de superposición flotante',
        'settings_dynamic_island_floating_permission_sub': 'Permite mostrar la cápsula sobre otras aplicaciones (compatible con OnePlus, Xiaomi, Vivo, Samsung)',
        'dynamic_island_speaker_on': 'ALTAVOZ',
        'dynamic_island_muted': 'Silenciado',
        'dynamic_island_recording': 'GRAB',
        'dynamic_island_tap_to_expand': 'Toca para controles de llamada',
        'callback_reminders_title': 'Recordatorios de devolución de llamada',
        'callback_reminders_stats': '%1$d activos, %2$d completados',
        'header_caller_id_privacy': 'ID de llamada saliente y privacidad (CLIR)',
        'settings_hide_caller_id': 'Ocultar ID de llamada',
        'settings_hide_caller_id_sub': 'Prefija llamadas con código del operador para ocultar tu número',
        'settings_clir_prefix': 'Prefijo de operador',
        'settings_clir_prefix_sub': 'Código activo: %1$s',
        'settings_clir_warning_title': 'Aviso normativo y del operador',
        'settings_clir_warning_desc': 'La compatibilidad con CLIR depende del operador y región. Los números de emergencia nunca se ocultan.',
        'settings_system_sim_caller_id': 'Ajustes SIM del sistema',
        'settings_system_sim_caller_id_sub': 'Abre las preferencias de ID de llamada del sistema Android',
        'settings_clir_prefix_dialog_title': 'Seleccionar prefijo CLIR',
        'settings_clir_prefix_gsm': 'Estándar GSM (#31#)',
        'settings_clir_prefix_us': 'Norteamérica (*67)',
        'settings_clir_prefix_uk': 'Reino Unido (141)',
        'settings_clir_prefix_japan': 'Japón (1831)',
        'settings_clir_prefix_custom': 'Prefijo personalizado',
        'settings_clir_custom_label': 'Introduce el código de prefijo CLIR de tu operador',
        'settings_pure_black_sub_disabled_dark': 'Negro puro #000000 para ahorro máximo en pantallas OLED (activa el tema oscuro)'
    },
    'fr': {
        'btn_grant': 'Accorder',
        'btn_configure': 'Configurer',
        'btn_set_default': 'Définir par défaut',
        'status_granted': 'Accordé',
        'status_enabled': 'Activé',
        'settings_lockscreen_calling_title': 'Appels plein écran sur l\'écran verrouillé',
        'settings_lockscreen_calling_sub': 'Affiche l\'interface d\'appel entrant en plein écran lorsque l\'appareil est verrouillé ou éteint',
        'settings_full_screen_intent_warning': 'Les alertes d\'appel plein écran sont restreintes par le système',
        'settings_overlay_permission_warning': 'Autorisation de superposition requise pour afficher la Dynamic Island sur d\'autres apps',
        'settings_all_call_permissions_good': 'Toutes les fonctionnalités d\'appel et d\'écran verrouillé sont configurées',
        'settings_recordings_always_on': 'Toujours enregistrer les appels',
        'settings_recordings_always_on_sub': 'Démarre automatiquement l\'enregistrement de chaque appel entrant et sortant',
        'settings_recordings_biometric_lock': 'Verrouiller les enregistrements avec la biométrie',
        'settings_recordings_biometric_lock_sub': 'Exige l\'empreinte digitale ou le verrouillage de l\'appareil pour accéder aux enregistrements audio',
        'settings_recordings_auto_export': 'Exporter automatiquement vers Téléchargements',
        'settings_recordings_auto_export_sub': 'Copie chaque enregistrement dans Téléchargements/SecureDialer pour un accès direct',
        'settings_recordings_compression_title': 'Compression et qualité audio',
        'settings_recordings_compression_sub': 'Optimise le débit et l\'échantillonnage pour équilibrer fidélité sonore et espace de stockage',
        'settings_recordings_cleanup_empty': 'Nettoyer les enregistrements vides/inaudibles',
        'vault_locked_title': 'Coffre d\'enregistrements verrouillé',
        'vault_locked_desc': 'L\'authentification biométrique est requise pour accéder aux enregistrements confidentiels.',
        'btn_unlock_vault': 'Déverrouiller le coffre',
        'settings_dynamic_island_title': 'Capsule d\'appel Dynamic Island',
        'settings_dynamic_island_sub': 'Affiche une pilule d\'appel flottante interactive autour de l\'encoche caméra',
        'settings_dynamic_island_speaker_only': 'Afficher uniquement sur haut-parleur',
        'settings_dynamic_island_speaker_only_sub': 'Affiche la Dynamic Island exclusivement lorsque le haut-parleur est actif',
        'settings_dynamic_island_floating_permission': 'Autorisation de superposition flottante',
        'settings_dynamic_island_floating_permission_sub': 'Autorise la capsule à flotter sur les autres applications (compatible OnePlus, Xiaomi, Vivo, Samsung)',
        'dynamic_island_speaker_on': 'HAUT-PARLEUR',
        'dynamic_island_muted': 'Muet',
        'dynamic_island_recording': 'ENR',
        'dynamic_island_tap_to_expand': 'Toucher pour développer les commandes',
        'callback_reminders_title': 'Rappels de rappel téléphonique',
        'callback_reminders_stats': '%1$d actifs, %2$d terminés',
        'header_caller_id_privacy': 'Numéro d\'appel sortant et confidentialité (CLIR)',
        'settings_hide_caller_id': 'Masquer l\'identifiant de l\'appelant',
        'settings_hide_caller_id_sub': 'Préfixe les appels avec le code opérateur pour masquer votre numéro',
        'settings_clir_prefix': 'Préfixe opérateur',
        'settings_clir_prefix_sub': 'Code actif : %1$s',
        'settings_clir_warning_title': 'Avis réglementaire et opérateur',
        'settings_clir_warning_desc': 'La compatibilité CLIR dépend de votre opérateur et région. Les numéros d\'urgence ne sont jamais masqués.',
        'settings_system_sim_caller_id': 'Paramètres SIM du système',
        'settings_system_sim_caller_id_sub': 'Ouvre les préférences Android de présentation du numéro',
        'settings_clir_prefix_dialog_title': 'Sélectionner le préfixe CLIR',
        'settings_clir_prefix_gsm': 'Standard GSM (#31#)',
        'settings_clir_prefix_us': 'Amérique du Nord (*67)',
        'settings_clir_prefix_uk': 'Royaume-Uni (141)',
        'settings_clir_prefix_japan': 'Japon (1831)',
        'settings_clir_prefix_custom': 'Préfixe personnalisé',
        'settings_clir_custom_label': 'Saisissez le préfixe CLIR de votre opérateur',
        'settings_pure_black_sub_disabled_dark': 'Noir pur #000000 pour une économie maximale sur écran OLED (active le thème sombre)'
    }
}

def parse_structure(xml_path):
    """Parses XML into ordered elements (comments and strings)."""
    with open(xml_path, 'r', encoding='utf-8') as f:
        content = f.read()

    items = []
    # Match comments or string tags
    token_pattern = re.compile(r'(<!--.*?-->)|(<string\s+name="([^"]+)"[^>]*>(.*?)</string>)', re.DOTALL)
    for m in token_pattern.finditer(content):
        comment, full_tag, str_name, str_val = m.groups()
        if comment:
            items.append(('comment', comment.strip()))
        elif full_tag:
            items.append(('string', str_name, str_val))
    return items

def load_kv(xml_path):
    """Loads key-value pairs from strings.xml."""
    tree = ET.parse(xml_path)
    kv = {}
    for el in tree.getroot().findall('string'):
        kv[el.get('name')] = el.text or ''
    return kv

def sync_locale(target_file, lang_code, master_structure, master_kv):
    existing_kv = load_kv(target_file) if os.path.exists(target_file) else {}
    lang_translations = NEW_TRANSLATIONS.get(lang_code, {})

    lines = [
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<!--",
        "  ~ Copyright (C) 2026 MovStore",
        "  ~ SPDX-License-Identifier: GPL-3.0-or-later",
        "  -->",
        "<resources>"
    ]

    for item in master_structure:
        if item[0] == 'comment':
            if 'Copyright' not in item[1]:
                lines.append(f"\n    {item[1]}")
        elif item[0] == 'string':
            key = item[1]
            master_val = master_kv[key]

            if key in existing_kv and existing_kv[key].strip():
                val = existing_kv[key]
            elif key in lang_translations:
                val = lang_translations[key]
            else:
                val = master_val

            val = escape_xml(val)
            lines.append(f"    <string name=\"{key}\">{val}</string>")

    lines.append("</resources>\n")
    
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print(f"Synchronized {target_file} ({lang_code}) - exact structure aligned.")

def main():
    master_structure = parse_structure(BASE_FILE)
    master_kv = load_kv(BASE_FILE)

    print(f"Loaded master English structure: {len(master_structure)} items, {len(master_kv)} strings.")

    for root, dirs, files in os.walk(RES_DIR):
        if 'values-' in root and 'strings.xml' in files:
            lang_code = os.path.basename(root).replace('values-', '')
            target_path = os.path.join(root, 'strings.xml')
            sync_locale(target_path, lang_code, master_structure, master_kv)

if __name__ == '__main__':
    main()
