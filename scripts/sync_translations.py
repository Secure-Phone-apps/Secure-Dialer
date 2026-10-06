#!/usr/bin/env python3
"""
Full Localization Architecture & Parity Synchronization Engine for Secure Dialer.

Harmonizes:
1. Canonical English (values/strings.xml) as Single Source of Truth.
2. 9 Localized Resources (values-{ar,de,es,fr,hi,ja,pl,pt,zh}/strings.xml).
3. 24 Pristine, Well-Structured Functional Sections with Identical Key Sequence.
4. XML Entity Escaping (&amp;, apostrophes, quotes, formatted="false").
5. Natural, High-Quality Local Language Translations.
"""

import os
import re
import xml.etree.ElementTree as ET

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES_DIR = os.path.join(BASE_DIR, "app", "src", "main", "res")

# Locales to synchronize
LOCALES = ["ar", "de", "es", "fr", "hi", "ja", "pl", "pt", "zh"]

# Translations dictionary for keys that need local phrasing
TRANSLATIONS = {
    # German
    "de": {
        "tab_spam_database": "Offline-Spam-Datenbank",
        "tab_scheduled_reminders": "Geplante Erinnerungen",
        "tab_fake_call_sim": "Täuschungsanruf-Simulator",
        "label_name": "Name",
        "header_theme_styling": "DESIGN &amp; STIL",
        "header_navigation_layout": "NAVIGATION &amp; LAYOUT",
        "header_community": "COMMUNITY",
        "app_version_name": "Version 1.5.0",
        "label_spam_label_hint": "Bezeichnung",
        "btn_import": "Importieren",
        "settings_merge_duplicate_contacts": "Doppelte Kontakte zusammenführen",
        "settings_merge_duplicate_contacts_sub": "Doppelte Kontakteinträge scannen und bereinigen",
        "settings_clir_prefix_japan": "Japan (1831)",
        "backup_service_health_title": "Sicherung &amp; Status",
        "service_health_active": "Aktiv &amp; Geschützt",
        "settings_recordings_compression_title": "Audiokompression &amp; Qualität",
        "header_caller_id_privacy": "Ausgehende Anrufer-ID &amp; Privatsphäre (CLIR)",
        "settings_clir_warning_title": "Hinweis zu Netzbetreibern &amp; Vorschriften",
        "settings_check_updates_sub": "Sicherstellen, dass die App aktuell &amp; sicher ist"
    },
    # Spanish
    "es": {
        "dialpad_create_contact": "Crear contacto",
        "dialpad_add_to_existing": "Añadir a existente",
        "dialpad_select_contact_title": "Seleccionar contacto",
        "dialpad_search_contact_hint": "Buscar contactos...",
        "dialpad_no_contacts_found": "No se encontraron contactos coincidentes",
        "voicemail_settings": "Ajustes de buzón de voz",
        "speed_dial": "Marcación rápida",
        "settings_auto_tune_volume": "Ajuste automático de volumen para grabación",
        "settings_auto_tune_volume_sub": "Equilibra automáticamente el volumen del altavoz al 55% durante la grabación para una captura acústica nítida.",
        "settings_recording_chime": "Tono y vibración de grabación",
        "settings_recording_chime_sub": "Reproduce un tono sutil y respuesta háptica al iniciar o detener la grabación",
        "share_recording_title": "Compartir y exportar grabación",
        "share_via_apps": "Compartir mediante aplicaciones",
        "share_via_apps_sub": "Enviar archivo de audio a mensajería, nube o aplicaciones externas",
        "save_to_downloads": "Guardar en Descargas",
        "save_to_downloads_sub": "Exportar una copia directamente a la carpeta Descargas del dispositivo",
        "recording_details": "Detalles de grabación",
        "headset_recording_warning": "Auriculares conectados: solo se grabará tu voz a menos que cambies a altavoz.",
        "delete": "Eliminar",
        "summary_range_total": "Total",
        "settings_merge_duplicate_contacts": "Combinar contactos duplicados",
        "settings_merge_duplicate_contacts_sub": "Escanear y limpiar entradas de contactos duplicadas"
    },
    # French
    "fr": {
        "tab_contacts": "Contacts",
        "contacts_header": "Contacts",
        "label_type": "Type",
        "label_mobile": "Mobile",
        "label_notes": "Notes",
        "dialpad_create_contact": "Créer un contact",
        "dialpad_add_to_existing": "Ajouter à l\'existant",
        "dialpad_select_contact_title": "Sélectionner un contact",
        "dialpad_search_contact_hint": "Rechercher des contacts...",
        "dialpad_no_contacts_found": "Aucun contact correspondant",
        "action_message": "Message",
        "action_swipe_message": "Message",
        "voicemail_settings": "Paramètres de messagerie vocale",
        "speed_dial": "Numérotation rapide",
        "settings_auto_tune_volume": "Ajustement automatique du volume pour enregistrement",
        "settings_auto_tune_volume_sub": "Équilibre automatiquement le volume du haut-parleur à 55% pendant l\'enregistrement pour une capture audio claire.",
        "settings_recording_chime": "Tonalité et vibration d\'enregistrement",
        "settings_recording_chime_sub": "Émet un son discret et un retour haptique au début et à la fin de l\'enregistrement",
        "share_recording_title": "Partager et exporter l\'enregistrement",
        "share_via_apps": "Partager via des applications",
        "share_via_apps_sub": "Envoyer le fichier audio vers messagerie, cloud ou autres applications",
        "save_to_downloads": "Enregistrer dans Téléchargements",
        "save_to_downloads_sub": "Exporter une copie directement dans le dossier Téléchargements de l\'appareil",
        "recording_details": "Détails de l\'enregistrement",
        "headset_recording_warning": "Écouteurs connectés: seule votre voix sera enregistrée sauf si vous passez sur le haut-parleur.",
        "settings_contribution": "Contribution",
        "app_version_name": "Version 1.5.0",
        "delete": "Supprimer",
        "summary_range_total": "Total",
        "cat_call_blocking": "Blocage d\'appels &amp; Spam",
        "cat_voicemail_tools": "Messagerie vocale &amp; Enregistrement",
        "settings_security_protection": "Sécurité &amp; Protection",
        "settings_merge_duplicate_contacts": "Fusionner les contacts en double",
        "settings_merge_duplicate_contacts_sub": "Rechercher et nettoyer les doublons de contacts"
    },
    # Polish
    "pl": {
        "label_email": "E-mail",
        "settings_m3_expressive": "Material 3 Expressive",
        "settings_merge_duplicate_contacts": "Scal duplikaty kontaktów",
        "settings_merge_duplicate_contacts_sub": "Skanuj i usuwaj zduplikowane wpisy kontaktów"
    },
    # Chinese
    "zh": {
        "voicemail_settings": "语音信箱设置",
        "settings_m3_expressive": "Material 3 表现力设计",
        "speed_dial": "快速拨号",
        "delete": "删除",
        "settings_pure_black_sub_disabled_dark": "纯黑 #000000 极夜模式，最大化节省 OLED 电量（需启用深色主题）",
        "settings_recordings_always_on": "自动通话录音",
        "settings_recordings_always_on_sub": "接通任何来电或去电时自动启动录音",
        "settings_recordings_biometric_lock": "生物识别锁定录音",
        "settings_recordings_biometric_lock_sub": "需要指纹或设备凭据才可查看和播放录音",
        "settings_recordings_auto_export": "自动导出至下载目录",
        "settings_recordings_auto_export_sub": "录音结束后自动复制文件到 Downloads/SecureDialer 便于管理",
        "settings_recordings_compression_title": "音频压缩与音质",
        "settings_recordings_compression_sub": "优化音频编码码率与采样率，在保真度与存储空间之间取得平衡",
        "settings_recordings_cleanup_empty": "清理空白/静音录音",
        "vault_locked_title": "录音保险箱已锁定",
        "vault_locked_desc": "需通过生物识别验证方可访问机密通话录音。",
        "btn_unlock_vault": "解锁保险箱",
        "settings_dynamic_island_title": "灵动岛通话胶囊",
        "settings_dynamic_island_sub": "通话进行时在屏幕前置摄像头周围悬浮交互式通话胶囊",
        "settings_dynamic_island_speaker_only": "仅在免提扬声器时显示",
        "settings_dynamic_island_speaker_only_sub": "仅在开启扬声器免提模式时显示灵动岛",
        "settings_dynamic_island_floating_permission": "悬浮窗悬停权限",
        "settings_dynamic_island_floating_permission_sub": "允许通话胶囊悬浮于其他应用之上（支持小米、OPPO、vivo、三星等）",
        "dynamic_island_speaker_on": "免提开启",
        "dynamic_island_muted": "已静音",
        "dynamic_island_recording": "录音中",
        "dynamic_island_tap_to_expand": "轻触展开通话控制",
        "callback_reminders_title": "回电提醒",
        "callback_reminders_stats": "%1$d 个待处理，%2$d 个已完成",
        "header_caller_id_privacy": "去电主叫号码与隐私 (CLIR)",
        "settings_hide_caller_id": "隐藏主叫号码",
        "settings_hide_caller_id_sub": "在去电号码前添加运营商隐私前缀以隐藏您的手机号",
        "settings_clir_prefix": "运营商隐私前缀",
        "settings_clir_prefix_sub": "当前代码：%1$s",
        "settings_clir_warning_title": "运营商与监管提示",
        "settings_clir_warning_desc": "CLIR 隐私支持因运营商和地区而异。紧急求助电话绝不会被隐藏。",
        "settings_system_sim_caller_id": "系统 SIM 卡设置",
        "settings_system_sim_caller_id_sub": "打开 Android 系统电话主叫号码偏好设置",
        "settings_clir_prefix_dialog_title": "选择 CLIR 前缀代码",
        "settings_clir_prefix_gsm": "GSM 标准 (#31#)",
        "settings_clir_prefix_us": "北美 (*67)",
        "settings_clir_prefix_uk": "英国 (141)",
        "settings_clir_prefix_japan": "日本 (1831)",
        "settings_clir_prefix_custom": "自定义前缀",
        "settings_clir_custom_label": "输入自定义运营商 CLIR 代码",
        "btn_grant": "授予",
        "btn_configure": "配置",
        "btn_set_default": "设为默认",
        "status_granted": "已授予",
        "status_enabled": "已启用",
        "settings_lockscreen_calling_title": "锁屏全屏来电界面",
        "settings_lockscreen_calling_sub": "设备锁屏或息屏时立即显示全屏来电接听界面",
        "settings_full_screen_intent_warning": "全屏来电通知受到系统限制",
        "settings_overlay_permission_warning": "需要悬浮窗权限以在其他应用上方显示灵动岛",
        "settings_all_call_permissions_good": "所有通话与锁屏功能均已完全配置",
        "settings_merge_duplicate_contacts": "合并重复联系人",
        "settings_merge_duplicate_contacts_sub": "扫描并清理重复联系人条目",
        "search_results_count": "搜索结果 (%1$d)",
        "no_matching_settings": "没有匹配的设置项",
        "no_matching_settings_desc": "尝试搜索深色模式、SIM 卡、拦截或录音等关键词。"
    },
    # Japanese
    "ja": {
        "dialpad_create_contact": "連絡先を作成",
        "dialpad_add_to_existing": "既存の連絡先に追加",
        "dialpad_select_contact_title": "連絡先を選択",
        "dialpad_search_contact_hint": "連絡先を検索...",
        "dialpad_no_contacts_found": "一致する連絡先はありません",
        "voicemail_settings": "留守番電話設定",
        "speed_dial": "短縮ダイヤル",
        "settings_auto_tune_volume": "録音用音量自動調整",
        "settings_auto_tune_volume_sub": "録音中はスピーカー音量を自動で55%に調整し、明瞭な双方向音声を記録します。",
        "settings_recording_chime": "録音チャイム音と触覚",
        "settings_recording_chime_sub": "通話録音の開始時と終了時に控えめな音と触覚フィードバックを再生",
        "share_recording_title": "録音を共有・エクスポート",
        "share_via_apps": "アプリで共有",
        "share_via_apps_sub": "メッセージ、クラウド、または外部アプリに音声ファイルを送信",
        "save_to_downloads": "ダウンロードに保存",
        "save_to_downloads_sub": "端末のダウンロードフォルダにコピーを直接エクスポート",
        "recording_details": "録音の詳細",
        "headset_recording_warning": "イヤホン接続中: スピーカーに切り替えない限り、自分の声のみ録音されます。",
        "delete": "削除",
        "summary_range_total": "合計",
        "settings_m3_expressive": "Material 3 Expressive",
        "settings_pure_black_sub_disabled_dark": "OLEDバッテリー節約のための純黒 #000000（ダークテーマを有効化）",
        "settings_recordings_always_on": "常に通話を録音",
        "settings_recordings_always_on_sub": "すべての発信・着信通話の接続時に自動で録音を開始",
        "settings_recordings_biometric_lock": "生体認証で録音を保護",
        "settings_recordings_biometric_lock_sub": "録音の表示・再生に指紋や端末の認証情報を要求",
        "settings_recordings_auto_export": "ダウンロードに自動エクスポート",
        "settings_recordings_auto_export_sub": "通話終了時に録音ファイルをDownloads/SecureDialerに自動コピー",
        "settings_recordings_compression_title": "音声圧縮と音質",
        "settings_recordings_compression_sub": "音声ビットレートとサンプリングレートを最適化し、音質と容量を両立",
        "settings_recordings_cleanup_empty": "無音・空の録音を削除",
        "vault_locked_title": "録音保管庫はロックされています",
        "vault_locked_desc": "機密通話録音にアクセスするには生体認証が必要です。",
        "btn_unlock_vault": "保管庫のロック解除",
        "settings_dynamic_island_title": "ダイナミックアイランド通話カプセル",
        "settings_dynamic_island_sub": "通話中、カメラ周囲に対話型の通話ピルを表示",
        "settings_dynamic_island_speaker_only": "スピーカー時のみ表示",
        "settings_dynamic_island_speaker_only_sub": "スピーカー通話中のみダイナミックアイランドを表示",
        "settings_dynamic_island_floating_permission": "フローティング表示の権限",
        "settings_dynamic_island_floating_permission_sub": "他のアプリの上にカプセルを表示（Galaxy、Xiaomi、Xperia等に対応）",
        "dynamic_island_speaker_on": "スピーカーON",
        "dynamic_island_muted": "ミュート中",
        "dynamic_island_recording": "録音中",
        "dynamic_island_tap_to_expand": "タップして通話操作を展開",
        "callback_reminders_title": "折り返しリマインダー",
        "callback_reminders_stats": "%1$d件の有効、%2$d件の完了",
        "header_caller_id_privacy": "発信者番号とプライバシー (CLIR)",
        "settings_hide_caller_id": "発信者番号を非通知",
        "settings_hide_caller_id_sub": "キャリアのプレフィックス番号を付加して番号を非通知にします",
        "settings_clir_prefix": "キャリアプレフィックス",
        "settings_clir_prefix_sub": "現在のコード: %1$s",
        "settings_clir_warning_title": "キャリアおよび規制に関する注意事項",
        "settings_clir_warning_desc": "CLIRの対応状況は地域やキャリアによって異なります。緊急通報は非通知になりません。",
        "settings_system_sim_caller_id": "システムSIM設定",
        "settings_system_sim_caller_id_sub": "Androidシステムの発信者番号設定を開きます",
        "settings_clir_prefix_dialog_title": "CLIRプレフィックスを選択",
        "settings_clir_prefix_gsm": "GSM標準 (#31#)",
        "settings_clir_prefix_us": "北米 (*67)",
        "settings_clir_prefix_uk": "英国 (141)",
        "settings_clir_prefix_japan": "日本 (1831)",
        "settings_clir_prefix_custom": "カスタムプレフィックス",
        "settings_clir_custom_label": "カスタムキャリアCLIRコードを入力",
        "btn_grant": "許可",
        "btn_configure": "設定",
        "btn_set_default": "デフォルトに設定",
        "status_granted": "許可済み",
        "status_enabled": "有効",
        "settings_lockscreen_calling_title": "ロック画面での全画面着信",
        "settings_lockscreen_calling_sub": "端末がロック中や画面オフでも即座に全画面着信画面を表示",
        "settings_full_screen_intent_warning": "全画面着信通知がシステムによって制限されています",
        "settings_overlay_permission_warning": "他のアプリの上にダイナミックアイランドを表示するには権限が必要です",
        "settings_all_call_permissions_good": "すべての通話・ロック画面機能が正常に設定されています",
        "settings_merge_duplicate_contacts": "重複連絡先を統合",
        "settings_merge_duplicate_contacts_sub": "重複した連絡先エントリーをスキャンして整理",
        "search_results_count": "検索結果 (%1$d件)",
        "no_matching_settings": "該当する設定がありません",
        "no_matching_settings_desc": "ダークモード、SIM、着信拒否、録音などのキーワードでお試しください。"
    },
    # Portuguese
    "pt": {
        "dialpad_create_contact": "Criar contato",
        "dialpad_add_to_existing": "Adicionar a existente",
        "dialpad_select_contact_title": "Selecionar contato",
        "dialpad_search_contact_hint": "Buscar contatos...",
        "dialpad_no_contacts_found": "Nenhum contato encontrado",
        "voicemail_settings": "Configurações de correio de voz",
        "speed_dial": "Discagem rápida",
        "settings_auto_tune_volume": "Ajuste automático de volume para gravação",
        "settings_auto_tune_volume_sub": "Equilibra o volume do alto-falante em 55% durante a gravação para captura acústica nítida.",
        "settings_recording_chime": "Toque e vibração de gravação",
        "settings_recording_chime_sub": "Reproduz som discreto e vibração quando a gravação de chamada inicia ou para",
        "share_recording_title": "Compartilhar e exportar gravação",
        "share_via_apps": "Compartilhar via aplicativos",
        "share_via_apps_sub": "Enviar arquivo de áudio para mensagens, nuvem ou outros apps",
        "save_to_downloads": "Salvar em Downloads",
        "save_to_downloads_sub": "Exportar uma cópia diretamente para a pasta Downloads do aparelho",
        "recording_details": "Detalhes da gravação",
        "headset_recording_warning": "Fones conectados: apenas sua voz será gravada, a menos que mude para o viva-voz.",
        "delete": "Excluir",
        "summary_range_total": "Total",
        "settings_pure_black_sub_disabled_dark": "Preto puro #000000 para economia de bateria OLED (ativa o Tema Escuro)",
        "settings_recordings_always_on": "Sempre gravar chamadas",
        "settings_recordings_always_on_sub": "Inicia a gravação automaticamente assim que uma chamada é atendida",
        "settings_recordings_biometric_lock": "Bloquear gravações com biometria",
        "settings_recordings_biometric_lock_sub": "Exige impressão digital ou credencial para ouvir as gravações",
        "settings_recordings_auto_export": "Exportar automaticamente para Downloads",
        "settings_recordings_auto_export_sub": "Copia automaticamente as gravações finalizadas para Downloads/SecureDialer",
        "settings_recordings_compression_title": "Compressão e qualidade de áudio",
        "settings_recordings_compression_sub": "Otimiza a taxa de bits e amostragem para equilibrar fidelidade e espaço",
        "settings_recordings_cleanup_empty": "Limpar gravações vazias/silenciosas",
        "vault_locked_title": "Cofre de gravações bloqueado",
        "vault_locked_desc": "Autenticação biométrica é necessária para acessar as gravações confidenciais.",
        "btn_unlock_vault": "Desbloquear cofre",
        "settings_dynamic_island_title": "Cápsula Dynamic Island",
        "settings_dynamic_island_sub": "Exibe uma pílula interativa ao redor da câmera durante chamadas ativas",
        "settings_dynamic_island_speaker_only": "Mostrar apenas no viva-voz",
        "settings_dynamic_island_speaker_only_sub": "Exibe a Dynamic Island exclusivamente quando o viva-voz estiver ativo",
        "settings_dynamic_island_floating_permission": "Permissão de sobreposição flutuante",
        "settings_dynamic_island_floating_permission_sub": "Permite que a cápsula flutue sobre outros aplicativos (OnePlus, Xiaomi, Samsung, etc.)",
        "dynamic_island_speaker_on": "VIVA-VOZ",
        "dynamic_island_muted": "Mudo",
        "dynamic_island_recording": "GRAV",
        "dynamic_island_tap_to_expand": "Toque para expandir controles de chamada",
        "callback_reminders_title": "Lembretes de retorno de chamada",
        "callback_reminders_stats": "%1$d ativos, %2$d concluídos",
        "header_caller_id_privacy": "Identificador de chamadas e privacidade (CLIR)",
        "settings_hide_caller_id": "Ocultar número de chamada",
        "settings_hide_caller_id_sub": "Adiciona o prefixo da operadora para ocultar seu número nas chamadas saintes",
        "settings_clir_prefix": "Prefixo da operadora",
        "settings_clir_prefix_sub": "Código ativo: %1$s",
        "settings_clir_warning_title": "Aviso da operadora e regulatório",
        "settings_clir_warning_desc": "O suporte ao CLIR varia conforme a operadora e região. Números de emergência nunca são ocultados.",
        "settings_system_sim_caller_id": "Configurações de SIM do sistema",
        "settings_system_sim_caller_id_sub": "Abre as preferências de identificador de chamadas do Android",
        "settings_clir_prefix_dialog_title": "Selecionar prefixo CLIR",
        "settings_clir_prefix_gsm": "Padrão GSM (#31#)",
        "settings_clir_prefix_us": "América do Norte (*67)",
        "settings_clir_prefix_uk": "Reino Unido (141)",
        "settings_clir_prefix_japan": "Japão (1831)",
        "settings_clir_prefix_custom": "Prefixo personalizado",
        "settings_clir_custom_label": "Insira o código de prefixo CLIR da operadora",
        "btn_grant": "Conceder",
        "btn_configure": "Configurar",
        "btn_set_default": "Definir padrão",
        "status_granted": "Concedido",
        "status_enabled": "Ativado",
        "settings_lockscreen_calling_title": "Chamadas em tela cheia na tela de bloqueio",
        "settings_lockscreen_calling_sub": "Exibe tela de chamada recebida instantaneamente com tela bloqueada ou apagada",
        "settings_full_screen_intent_warning": "Alertas de chamada em tela cheia restritos pelo sistema",
        "settings_overlay_permission_warning": "Permissão de sobreposição necessária para a Dynamic Island flutuar sobre outros apps",
        "settings_all_call_permissions_good": "Todas as funções de chamada e tela de bloqueio estão configuradas",
        "settings_merge_duplicate_contacts": "Mesclar contatos duplicados",
        "settings_merge_duplicate_contacts_sub": "Examinar e limpar contatos repetidos",
        "search_results_count": "Resultados da busca (%1$d)",
        "no_matching_settings": "Nenhuma configuração encontrada",
        "no_matching_settings_desc": "Tente buscar por Modo Escuro, SIM, Bloqueio ou Gravação."
    },
    # Hindi
    "hi": {
        "dialpad_create_contact": "संपर्क बनाएं",
        "dialpad_add_to_existing": "मौजूदा में जोड़ें",
        "dialpad_select_contact_title": "संपर्क चुनें",
        "dialpad_search_contact_hint": "संपर्क खोजें...",
        "dialpad_no_contacts_found": "कोई मिलता-जुलता संपर्क नहीं मिला",
        "voicemail_settings": "वॉइसमेल सेटिंग्स",
        "speed_dial": "स्पीड डायल",
        "settings_auto_tune_volume": "रिकॉर्डिंग के लिए स्पीकर वॉल्यूम स्वतः ट्यून करें",
        "settings_auto_tune_volume_sub": "स्पष्ट आवाज़ रिकॉर्डिंग के लिए कॉल के दौरान स्पीकर वॉल्यूम को स्वतः 55% पर संतुलित करता है।",
        "settings_recording_chime": "रिकॉर्डिंग ऑडियो टोन और कंपन",
        "settings_recording_chime_sub": "कॉल रिकॉर्डिंग शुरू या बंद होने पर हल्का ऑडियो टोन और स्पर्श प्रतिक्रिया बजाएं",
        "share_recording_title": "रिकॉर्डिंग साझा और निर्यात करें",
        "share_via_apps": "ऐप्स के माध्यम से साझा करें",
        "share_via_apps_sub": "ऑडियो फ़ाइल को मैसेजिंग, क्लाउड या अन्य ऐप्स पर भेजें",
        "save_to_downloads": "डाउनलोड में सहेजें",
        "save_to_downloads_sub": "सीधे डिवाइस के डाउनलोड फ़ोल्डर में प्रतिलिपि निर्यात करें",
        "recording_details": "रिकॉर्डिंग विवरण",
        "headset_recording_warning": "हेडफ़ोन कनेक्टेड हैं: स्पीकर पर स्विच किए बिना केवल आपकी आवाज़ रिकॉर्ड होगी।",
        "delete": "हटाएं",
        "summary_range_total": "कुल",
        "settings_pure_black_sub_disabled_dark": "OLED बैटरी की अधिकतम बचत के लिए शुद्ध काला #000000 (डार्क थीम सक्षम करता है)",
        "settings_recordings_always_on": "हमेशा कॉल रिकॉर्ड करें",
        "settings_recordings_always_on_sub": "कनेक्ट होते ही हर इनकमिंग और आउटगोइंग कॉल को स्वतः रिकॉर्ड करना शुरू करें",
        "settings_recordings_biometric_lock": "बायोमेट्रिक्स से रिकॉर्डिंग लॉक करें",
        "settings_recordings_biometric_lock_sub": "कॉल रिकॉर्डिंग देखने और सुनने के लिए फ़िंगरप्रिंट या पिन की आवश्यकता होगी",
        "settings_recordings_auto_export": "डाउनलोड में स्वतः निर्यात करें",
        "settings_recordings_auto_export_sub": "फ़ाइल प्रबंधक में तत्काल पहुंच के लिए हर रिकॉर्डिंग को Downloads/SecureDialer में कॉपी करें",
        "settings_recordings_compression_title": "ऑडियो संपीड़न और गुणवत्ता",
        "settings_recordings_compression_sub": "स्टोरेज स्पेस और ऑडियो स्पष्टता में संतुलन बनाने के लिए बिटरेट को अनुकूलित करें",
        "settings_recordings_cleanup_empty": "खाली/शांत रिकॉर्डिंग साफ़ करें",
        "vault_locked_title": "रिकॉर्डिंग वॉल्ट लॉक है",
        "vault_locked_desc": "गोपनीय कॉल रिकॉर्डिंग तक पहुँचने के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है।",
        "btn_unlock_vault": "वॉल्ट अनलॉक करें",
        "settings_dynamic_island_title": "डायनामिक आइलैंड कॉल कैप्सूल",
        "settings_dynamic_island_sub": "चल रही कॉल के लिए कैमरा कटआउट के पास एक इंटरैक्टिव फ्लोटिंग पिल दिखाएं",
        "settings_dynamic_island_speaker_only": "केवल स्पीकर पर दिखाएं",
        "settings_dynamic_island_speaker_only_sub": "डायनामिक आइलैंड केवल तब दिखाएं जब स्पीकरफ़ोन सक्रिय हो",
        "settings_dynamic_island_floating_permission": "फ्लोटिंग ओवरले अनुमति",
        "settings_dynamic_island_floating_permission_sub": "अन्य ऐप्स के ऊपर कैप्सूल प्रदर्शित करने की अनुमति दें",
        "dynamic_island_speaker_on": "स्पीकर चालू",
        "dynamic_island_muted": "म्यूट",
        "dynamic_island_recording": "रिकॉर्ड",
        "dynamic_island_tap_to_expand": "कॉल नियंत्रण खोलने के लिए टैप करें",
        "callback_reminders_title": "कॉलबैक रिमाइंडर",
        "callback_reminders_stats": "%1$d सक्रिय, %2$d पूर्ण",
        "header_caller_id_privacy": "आउटगोइंग कॉलर आईडी और गोपनीयता (CLIR)",
        "settings_hide_caller_id": "कॉलर आईडी छुपाएं",
        "settings_hide_caller_id_sub": "अपना नंबर छिपाने के लिए आउटगोइंग कॉल में कैरियर गोपनीयता कोड जोड़ें",
        "settings_clir_prefix": "कैरियर उपसर्ग",
        "settings_clir_prefix_sub": "सक्रिय कोड: %1$s",
        "settings_clir_warning_title": "कैरियर और विनियामक सूचना",
        "settings_clir_warning_desc": "CLIR समर्थन कैरियर और क्षेत्र के अनुसार भिन्न होता है। आपातकालीन नंबर कभी नहीं छिपाए जाते।",
        "settings_system_sim_caller_id": "सिस्टम सिम सेटिंग्स",
        "settings_system_sim_caller_id_sub": "एंड्रॉइड सिस्टम कॉलर आईडी प्राथमिकताएं खोलें",
        "settings_clir_prefix_dialog_title": "CLIR उपसर्ग चुनें",
        "settings_clir_prefix_gsm": "जीएसएम मानक (#31#)",
        "settings_clir_prefix_us": "उत्तरी अमेरिका (*67)",
        "settings_clir_prefix_uk": "यूनाइटेड किंगडम (141)",
        "settings_clir_prefix_japan": "जापान (1831)",
        "settings_clir_prefix_custom": "कस्टम उपसर्ग",
        "settings_clir_custom_label": "कस्टम कैरियर CLIR उपसर्ग कोड दर्ज करें",
        "btn_grant": "अनुमति दें",
        "btn_configure": "कॉन्फ़िगर करें",
        "btn_set_default": "डिफ़ॉल्ट सेट करें",
        "status_granted": "स्वीकृत",
        "status_enabled": "सक्षम",
        "settings_lockscreen_calling_title": "लॉक स्क्रीन पर फ़ुल-स्क्रीन कॉल",
        "settings_lockscreen_calling_sub": "डिवाइस लॉक होने या स्क्रीन बंद होने पर तुरंत इनकमिंग कॉल स्क्रीन दिखाएं",
        "settings_full_screen_intent_warning": "फुल-स्क्रीन इनकमिंग कॉल अलर्ट सिस्टम द्वारा प्रतिबंधित हैं",
        "settings_overlay_permission_warning": "डायनामिक आइलैंड को अन्य ऐप्स के ऊपर दिखाने के लिए ओवरले अनुमति आवश्यक है",
        "settings_all_call_permissions_good": "सभी कॉलिंग और लॉक स्क्रीन सुविधाएं पूरी तरह से कॉन्फ़िगर हैं",
        "settings_merge_duplicate_contacts": "डुप्लिकेट संपर्क मर्ज करें",
        "settings_merge_duplicate_contacts_sub": "डुप्लिकेट संपर्क प्रविष्टियों को स्कैन और साफ़ करें",
        "search_results_count": "खोज परिणाम (%1$d)",
        "no_matching_settings": "कोई मिलती-जुलती सेटिंग नहीं मिली",
        "no_matching_settings_desc": "डार्क मोड, सिम, ब्लॉकिंग या रिकॉर्डिंग जैसे शब्दों से खोजें।"
    },
    # Arabic
    "ar": {
        "tab_spam_database": "قاعدة بيانات البريد العشوائي دون اتصال",
        "tab_scheduled_reminders": "تذكيرات مجدولة",
        "tab_fake_call_sim": "محاكي المكالمات الوهمية",
        "privacy_section_1_title": "عدم الاتصال بالإنترنت أولاً",
        "privacy_section_1_desc": "يعمل التطبيق بدون أي اتصال بالإنترنت، ولا يرسل أي بيانات إلى خوادم خارجية.",
        "privacy_section_2_title": "تخزين مشفر محلياً",
        "privacy_section_2_desc": "يتم حفظ جميع سجلات المكالمات وجهات الاتصال وقوائم الحظر محلياً في قاعدة بيانات مشفرة.",
        "privacy_section_3_title": "أذونات شفافة تماماً",
        "privacy_section_3_desc": "يطلب Secure Dialer الأذونات الأساسية اللازمة فقط لوظائف الهاتف والمكالمات.",
        "active_reminders_title": "التذكيرات النشطة",
        "schedule_callback_reminder_title": "جدولة تذكير بإعادة الاتصال",
        "schedule_callback_reminder_sub": "تلقي إشعار للتذكير بإعادة الاتصال بهذه الجهة",
        "remind_call_back_prompt": "تذكير بإعادة الاتصال بـ %1$s بعد:",
        "optional_note_label": "ملاحظة اختيارية",
        "app_locked_title": "التطبيق مقفل",
        "app_locked_desc": "مطلوب المصادقة البيومترية أو رقم PIN لفتح تطبيق الهاتف.",
        "device_auth_subtitle": "تحقق من هويتك للمتابعة",
        "restricted_settings_title": "إعدادات مقيدة بالنظام",
        "btn_open_settings": "فتح الإعدادات",
        "vcf_migration_title": "نقل جهات الاتصال (vCard)",
        "vcf_migration_desc": "تصدير أو استيراد جهات الاتصال بسهولة عبر ملفات .vcf القياسية دون وسطاء سحابيين.",
        "btn_export_vcf": "تصدير إلى vCard",
        "btn_import_vcf": "استيراد من vCard",
        "vcf_exported_title": "تم تصدير جهات الاتصال",
        "vcf_exported_desc": "تم حفظ ملف جهات الاتصال بنجاح في مجلد التنزيلات.",
        "vcf_import_title": "استيراد جهات الاتصال",
        "vcf_import_desc": "حدد ملف .vcf لاستيراد جهات الاتصال إلى هاتفك بأمان.",
        "vcf_content_label": "محتوى vCard",
        "btn_import_contacts": "استيراد جهات الاتصال",
        "settings_pure_black": "الأسود النقي (OLED)",
        "settings_pure_black_sub": "توفير أقصى قدر من طاقة شاشات OLED بخلفية سوداء بالكامل #000000",
        "settings_pure_black_sub_disabled_dark": "خلفية سوداء بالكامل #000000 لتوفير طاقة شاشات OLED (يتطلب تفعيل المظهر الداكن)",
        "theme_custom_title": "لون تمييز مخصص",
        "theme_custom_desc": "اختر أي لون مخصص بدقة عالية لواجهة الاتصال ولوحة الأرقام",
        "theme_hex_code_label": "رمز اللون الست عشري (HEX)",
        "theme_hex_code_chip": "رمز اللون",
        "theme_apply": "تطبيق اللون",
        "btn_save_file": "حفظ الملف",
        "btn_open_file": "فتح الملف",
        "btn_export_blocklist": "تصدير قائمة الحظر",
        "btn_import_blocklist": "استيراد قائمة الحظر",
        "btn_export_spam_csv": "تصدير أرقام السبام (CSV)",
        "btn_import_spam_file": "استيراد ملف السبام",
        "btn_save_backup_file": "حفظ ملف النسخة الاحتياطية",
        "btn_restore_backup_file": "استعادة النسخة الاحتياطية",
        "btn_save_vcf_file": "حفظ ملف جهات الاتصال",
        "btn_choose_vcf_file": "اختيار ملف جهات الاتصال",
        "file_saved_success": "تم حفظ الملف بنجاح",
        "file_save_failed": "فشل حفظ الملف",
        "file_read_failed": "فشل قراءة الملف المحدد",
        "blocklist_import_count_success": "تم استيراد %1$d رقم محظور بنجاح!",
        "spam_import_count_success": "تم استيراد %1$d رقم سبام بنجاح!",
        "vcf_import_count_success": "تم استيراد جهات الاتصال بنجاح!",
        "blocklist_file_action_title": "النسخ الاحتياطي والنقل للملفات",
        "blocklist_file_action_desc": "تصدير الأرقام المحظورة إلى ملف أو استيراد أرقام من قوائم مجتمعية خارجية.",
        "header_contacts_import_export": "استيراد وتصدير جهات الاتصال",
        "settings_export_contacts_vcard": "تصدير جهات الاتصال (vCard)",
        "settings_import_contacts_vcard": "استيراد جهات الاتصال (vCard)",
        "settings_import_contacts_vcard_sub": "استعادة جهات الاتصال من ملف .vcf القياسي",
        "settings_backup_restore_sub": "استعادة سجلات المكالمات والإعدادات من ملف JSON مشفر",
        "tab_slot_left": "الخانة اليسرى",
        "tab_slot_middle": "الخانة الوسطى",
        "tab_slot_right": "الخانة اليمنى",
        "settings_merge_duplicate_contacts": "دمج جهات الاتصال المكررة",
        "settings_merge_duplicate_contacts_sub": "فحص وتنظيف الأسماء والأرقام المكررة في جهات الاتصال",
        "search_results_count": "نتائج البحث (%1$d)",
        "no_matching_settings": "لا توجد إعدادات مطابقة",
        "no_matching_settings_desc": "جرّب البحث عن مصطلحات مثل الوضع الداكن، شريحة SIM، الحظر، أو التسجيل.",
        "settings_recordings_always_on": "تسجيل المكالمات دائماً",
        "settings_recordings_always_on_sub": "بدء تسجيل كل مكالمة واردة وصادرة تلقائياً بمجرد اتصالها",
        "settings_recordings_biometric_lock": "قفل التسجيلات بالبصمة",
        "settings_recordings_biometric_lock_sub": "طلب البصمة أو رمز القفل للوصول إلى التسجيلات الصوتية وتشغيلها",
        "settings_recordings_auto_export": "تصدير تلقائي إلى التنزيلات",
        "settings_recordings_auto_export_sub": "نسخ كل تسجيل مكالمة تلقائياً إلى مجلد Downloads/SecureDialer لتسهيل الوصول",
        "settings_recordings_compression_title": "ضغط وجودة الصوت",
        "settings_recordings_compression_sub": "موازنة معدل البت والتردد للحفاظ على وضوح الصوت مع توفير مساحة التخزين",
        "settings_recordings_cleanup_empty": "تنظيف التسجيلات الفارغة أو الصامتة",
        "vault_locked_title": "خزنة التسجيلات مقفلة",
        "vault_locked_desc": "يلزم المصادقة البيومترية للوصول إلى تسجيلات المكالمات السرية.",
        "btn_unlock_vault": "فتح الخزنة",
        "settings_dynamic_island_title": "كبسولة الجزيرة التفاعلية (Dynamic Island)",
        "settings_dynamic_island_sub": "إظهار كبسولة اتصال عائمة وتفاعلية حول ثقب الكاميرا أثناء المكالمات الجارية",
        "settings_dynamic_island_speaker_only": "إظهار عند تشغيل مكبر الصوت فقط",
        "settings_dynamic_island_speaker_only_sub": "عرض الجزيرة التفاعلية حصرياً عندما يكون وضع مكبر الصوت نشطاً",
        "settings_dynamic_island_floating_permission": "إذن العرض فوق التطبيقات",
        "settings_dynamic_island_floating_permission_sub": "السماح للكبسولة بالطفو فوق التطبيقات الأخرى (يدعم سامسونج وشاومي وون بلس)",
        "dynamic_island_speaker_on": "مكبر الصوت قيد التشغيل",
        "dynamic_island_muted": "صامت",
        "dynamic_island_recording": "تسجيل",
        "dynamic_island_tap_to_expand": "اضغط لتوسيع عناصر التحكم بالمكالمة",
        "callback_reminders_title": "تذكيرات إعادة الاتصال",
        "callback_reminders_stats": "%1$d نشطة، %2$d مكتملة",
        "header_caller_id_privacy": "هوية المتصل والخصوصية الصادرة (CLIR)",
        "settings_hide_caller_id": "إخفاء رقم المتصل",
        "settings_hide_caller_id_sub": "إضافة بادئة الخصوصية لشبكة الاتصال لإخفاء رقمك في المكالمات الصادرة",
        "settings_clir_prefix": "بادئة شبكة الاتصال",
        "settings_clir_prefix_sub": "الرمز النشط: %1$s",
        "settings_clir_warning_title": "إشعار مزود الخدمة والتنظيمات",
        "settings_clir_warning_desc": "يختلف دعم إخفاء الرقم باختلاف الشبكة والمنطقة. لا يتم إخفاء أرقام الطوارئ مطلقاً.",
        "settings_system_sim_caller_id": "إعدادات شريحة SIM في النظام",
        "settings_system_sim_caller_id_sub": "فتح تفضيلات هوية المتصل في نظام أندرويد",
        "settings_clir_prefix_dialog_title": "اختيار بادئة CLIR",
        "settings_clir_prefix_gsm": "معيار GSM (#31#)",
        "settings_clir_prefix_us": "أمريكا الشمالية (*67)",
        "settings_clir_prefix_uk": "المملكة المتحدة (141)",
        "settings_clir_prefix_japan": "اليابان (1831)",
        "settings_clir_prefix_custom": "بادئة مخصصة",
        "settings_clir_custom_label": "أدخل رمز بادئة شبكة الاتصال المخصص",
        "btn_grant": "منح",
        "btn_configure": "تهيئة",
        "btn_set_default": "تعيين كافتراضي",
        "status_granted": "ممنوح",
        "status_enabled": "مفعّل",
        "settings_lockscreen_calling_title": "مكالمات ملء الشاشة على شاشة القفل",
        "settings_lockscreen_calling_sub": "عرض شاشة المكالمة الواردة بملء الشاشة فوراً عندما يكون الهاتف مقفلاً أو الشاشة مغلقة",
        "settings_full_screen_intent_warning": "تنبيهات المكالمات بملء الشاشة مقيدة بواسطة النظام",
        "settings_overlay_permission_warning": "يلزم إذن العرض فوق التطبيقات لعرض الجزيرة التفاعلية",
        "settings_all_call_permissions_good": "تم تهيئة جميع ميزات الاتصال وشاشة القفل بنجاح"
    }
}

# The 24 structured sections
SECTIONS = [
    {
        "title": "Application Identity",
        "keys": ["app_name"]
    },
    {
        "title": "Navigation Tabs",
        "keys": [
            "tab_favorites", "tab_recents", "tab_contacts", "tab_dialpad", "tab_voicemail",
            "tab_spam_database", "tab_scheduled_reminders", "tab_fake_call_sim", "tab_call_notes",
            "tab_slot_left", "tab_slot_middle", "tab_slot_right"
        ]
    },
    {
        "title": "Search & General UI",
        "keys": [
            "search_placeholder", "search_settings_placeholder", "search_clear",
            "search_results_count", "no_matching_settings", "no_matching_settings_desc",
            "no_results_title", "no_results_subtitle", "filter_all", "filter_missed",
            "filter_dialed", "filter_received", "default_dialer_warning"
        ]
    },
    {
        "title": "Permissions",
        "keys": [
            "perm_contacts_title", "perm_phone_title", "perm_desc", "enable_contacts_perm",
            "enable_call_log_perm", "contacts_perm_desc", "call_log_perm_desc"
        ]
    },
    {
        "title": "Contacts Management & Details",
        "keys": [
            "contacts_header", "no_contacts_title", "no_contacts_subtitle", "add_contact",
            "action_add_contact", "contact_details", "edit_contact", "delete_contact", "name_required", "number_required",
            "label_name", "label_phone_number", "label_first_name", "label_last_name",
            "label_email", "label_type", "label_mobile", "label_home", "label_work", "label_other",
            "label_notes", "label_address", "label_add_number", "label_add_email",
            "contact_storage_badge_phone", "contact_storage_badge_sim", "contact_storage_badge_google",
            "contact_source_select", "contact_source_phone", "contact_source_label", "saved_in_account",
            "add_contact_dialog_title", "edit_contact_dialog_title", "contact_multiple_numbers"
        ]
    },
    {
        "title": "Recents & Call History",
        "keys": [
            "recents_header", "no_call_log_title", "no_call_log_subtitle", "clear_history",
            "clear_history_confirm", "clear_history_desc", "clear_call_log_confirm_title",
            "clear_call_log_confirm_desc", "clear_history_with_number", "delete_call_log_entry",
            "delete_all_call_history", "call_type_incoming", "call_type_outgoing", "call_type_missed",
            "call_duration", "call_time_yesterday", "call_unknown", "unknown", "badge_today",
            "badge_yesterday", "badge_older", "recent_label_format", "calls_count", "call_count_label",
            "showing_last_5_calls", "call_summary_prefix", "today_call_summary"
        ]
    },
    {
        "title": "Dialpad & T9 Search",
        "keys": [
            "dialpad_enter_number", "dialpad_create_contact", "dialpad_add_to_existing",
            "dialpad_select_contact_title", "dialpad_search_contact_hint", "dialpad_no_contacts_found",
            "dialpad_no_matches", "dialpad_clear", "dialpad_paste", "dialpad_copy"
        ]
    },
    {
        "title": "In-Call Actions & States",
        "keys": [
            "incall_mute", "incall_speaker", "incall_earpiece", "incall_bluetooth", "incall_hold",
            "incall_keypad", "incall_record", "incall_end", "incall_speaker_on", "btn_answer",
            "btn_decline", "btn_hangup", "btn_swap", "btn_answer_hold", "call_status_ongoing",
            "call_status_dialing", "call_status_ringing", "call_status_connecting", "call_status_hold",
            "call_status_ended", "call_status_conference", "mute", "speaker", "bluetooth", "hold",
            "keypad", "record", "recording", "recording_saved", "recording_started",
            "action_call", "action_message", "action_swipe_call", "action_swipe_message",
            "action_copy_number", "action_send_email", "action_open_map", "toast_number_copied",
            "number_copied", "send_sms", "sms_sent", "sms_failed", "call_waiting_title",
            "call_waiting_notice", "call_waiting_incoming_from", "conference", "caller_verified", "no_answer"
        ]
    },
    {
        "title": "Favorites & Voicemail",
        "keys": [
            "favorites_header", "no_favorites_title", "no_favorites_subtitle", "add_to_favorites",
            "voicemail_header", "no_voicemails_title", "no_voicemails_subtitle", "call_voicemail",
            "voicemail_settings", "voicemail_directory_number", "voicemail_sample_transcript"
        ]
    },
    {
        "title": "Settings Categories",
        "keys": [
            "cat_general", "cat_appearance", "cat_sound_gestures", "cat_calling_accounts",
            "cat_speed_dial_quick_responses", "cat_call_blocking", "cat_voicemail_tools",
            "cat_contacts_data", "cat_advanced_features", "cat_privacy_security_about",
            "cat_about", "cat_appearance_color", "cat_appearance_color_sub",
            "cat_sound_gestures_sub", "cat_calling_accounts_sub", "cat_speed_dial_quick_responses_sub",
            "cat_call_blocking_sub", "cat_voicemail_tools_sub", "cat_contacts_data_sub",
            "cat_advanced_features_sub", "cat_privacy_security_about_sub", "settings_title", "settings_back",
            "settings_info_utilities"
        ]
    },
    {
        "title": "Settings Headers",
        "keys": [
            "header_general_experience", "header_display_theme", "header_audio_vibration",
            "header_sim_carrier", "header_incoming_alerts", "header_threat_intelligence",
            "header_call_blocker", "header_dialing_shortcuts", "header_call_decline_messages",
            "header_carrier_voicemail", "header_contacts_management", "header_data_backup",
            "header_privacy_protection", "header_call_recording", "header_app_specs",
            "header_navigation_layout", "header_community"
        ]
    },
    {
        "title": "Settings - General & Appearance",
        "keys": [
            "settings_carrier_call_settings", "settings_carrier_call_settings_sub",
            "settings_appearance", "settings_dark_theme", "settings_dark_theme_sub",
            "settings_dynamic_color", "settings_dynamic_color_sub", "settings_dynamic_color_sub_fallback",
            "settings_m3_expressive", "settings_m3_expressive_sub", "settings_accent_color",
            "theme_accent_color", "theme_custom_title", "theme_custom_desc",
            "theme_hex_code_label", "theme_hex_code_chip", "theme_apply",
            "settings_pure_black", "settings_pure_black_sub", "settings_pure_black_sub_disabled_dark",
            "settings_tab_layout", "settings_tab_layout_sub", "settings_startup_options",
            "settings_default_startup_tab"
        ]
    },
    {
        "title": "Settings - Sound, Vibration & Gestures",
        "keys": [
            "settings_sound_haptics", "settings_dialpad_tones", "settings_dialpad_tones_sub",
            "settings_vibrate", "settings_vibrate_sub", "settings_flip_to_silence",
            "settings_flip_to_silence_sub", "settings_swipe_actions", "settings_swipe_actions_sub",
            "settings_flash_alerts", "settings_flash_alerts_sub", "settings_auto_tune_volume",
            "settings_auto_tune_volume_sub", "header_theme_styling", "header_sound_haptics"
        ]
    },
    {
        "title": "Settings - Calling Accounts, SIM & Telephony",
        "keys": [
            "settings_calling_features", "settings_call_waiting", "settings_call_waiting_sub",
            "settings_sim_voicemail", "settings_preferred_sim", "settings_preferred_sim_sub",
            "sim_1", "sim_2", "sim_ask", "select_sim_card", "choose_sim_card"
        ]
    },
    {
        "title": "Settings - Outgoing Caller ID, Privacy & CLIR",
        "keys": [
            "header_caller_id_privacy", "settings_hide_caller_id", "settings_hide_caller_id_sub",
            "settings_clir_prefix", "settings_clir_prefix_sub", "settings_clir_warning_title",
            "settings_clir_warning_desc", "settings_system_sim_caller_id", "settings_system_sim_caller_id_sub",
            "settings_clir_prefix_dialog_title", "settings_clir_prefix_gsm", "settings_clir_prefix_us",
            "settings_clir_prefix_uk", "settings_clir_prefix_japan", "settings_clir_prefix_custom",
            "settings_clir_custom_label"
        ]
    },
    {
        "title": "Settings - Speed Dial & Quick Responses",
        "keys": [
            "settings_speed_dial", "settings_speed_dial_sub", "speed_dial", "speed_dial_desc",
            "assign_speed_dial_key", "assign_key", "unassigned_key", "press_and_hold_dialpad",
            "edit_key", "delete_key", "settings_quick_responses", "settings_quick_responses_sub",
            "settings_quick_resp_title", "quick_responses", "quick_decline_messages",
            "create_custom_reply", "add_message_template", "no_responses_saved_title",
            "no_responses_saved_desc", "delete_response", "send_quick_response"
        ]
    },
    {
        "title": "Settings - Carrier Voicemail & Preferences",
        "keys": [
            "settings_voicemail_num", "settings_voicemail_num_sub", "settings_voicemail_setup_title",
            "voicemail_setup_desc", "save_voicemail_number"
        ]
    },
    {
        "title": "Settings - Call Blocking, Spam Database & Offline Protection",
        "keys": [
            "settings_calls_blocking", "settings_blocked_numbers", "settings_blocked_numbers_sub",
            "settings_block_list_title", "enter_number_to_block", "block_this_number",
            "blocked_callers_header", "no_blocked_numbers_title", "no_blocked_numbers_desc",
            "block", "unblock", "block_number", "unblock_number", "call_blocked_toast",
            "settings_spam_protection", "settings_spam_protection_sub", "local_offline_protection",
            "local_offline_protection_sub", "spam_entries_label", "spam_numbers_blocked",
            "add_spam_manually_title", "label_phone_number_hint", "label_spam_label_hint",
            "btn_add_to_offline_db", "blocklist_empty_title", "blocklist_empty_desc",
            "blocked_offline_numbers_title", "btn_clear_all", "import_csv_dialog_title",
            "import_csv_dialog_desc", "load_sample_dataset", "btn_load_sample_list",
            "spam_csv_placeholder", "btn_csv_import", "spam_db_check_updates",
            "spam_db_checking", "spam_db_up_to_date", "blocklist_file_action_title",
            "blocklist_file_action_desc", "btn_export_blocklist", "btn_import_blocklist",
            "btn_export_spam_csv", "btn_import_spam_file", "blocklist_import_count_success",
            "spam_import_count_success"
        ]
    },
    {
        "title": "Settings - Call Recording Vault, Audio & Compression",
        "keys": [
            "settings_call_recording", "settings_call_recording_sub", "settings_recordings",
            "settings_recordings_sub", "settings_recordings_title", "settings_view_saved_recordings",
            "settings_view_saved_recordings_sub", "secure_local_recordings", "recordings_privacy_notice",
            "no_recordings_title", "no_recordings_desc", "share_recording", "delete_recording",
            "toast_deleted_recording", "settings_recording_chime", "settings_recording_chime_sub",
            "share_recording_title", "share_via_apps", "share_via_apps_sub", "save_to_downloads",
            "save_to_downloads_sub", "recording_details", "headset_recording_warning",
            "settings_recordings_always_on", "settings_recordings_always_on_sub",
            "settings_recordings_biometric_lock", "settings_recordings_biometric_lock_sub",
            "settings_recordings_auto_export", "settings_recordings_auto_export_sub",
            "settings_recordings_compression_title", "settings_recordings_compression_sub",
            "settings_recordings_cleanup_empty", "vault_locked_title", "vault_locked_desc", "btn_unlock_vault"
        ]
    },
    {
        "title": "Settings - Dynamic Island Call Capsule",
        "keys": [
            "settings_dynamic_island_title", "settings_dynamic_island_sub",
            "settings_dynamic_island_speaker_only", "settings_dynamic_island_speaker_only_sub",
            "settings_dynamic_island_floating_permission", "settings_dynamic_island_floating_permission_sub",
            "dynamic_island_speaker_on", "dynamic_island_muted", "dynamic_island_recording",
            "dynamic_island_tap_to_expand"
        ]
    },
    {
        "title": "Settings - Contacts, Deduplication & Storage",
        "keys": [
            "settings_contact_preferences", "settings_contacts_to_display", "settings_contacts_to_display_sub",
            "settings_default_account_for_new", "settings_default_account_for_new_sub",
            "settings_dedup", "settings_dedup_sub", "settings_dedup_title", "dedup_desc",
            "no_duplicates_title", "no_duplicates_desc", "merge_all_duplicates", "merge_group",
            "btn_add_merge", "toast_merged_all_duplicates", "toast_merged_duplicates_for",
            "settings_merge_duplicate_contacts", "settings_merge_duplicate_contacts_sub"
        ]
    },
    {
        "title": "Settings - Backup, Restore & Encrypted Migration",
        "keys": [
            "settings_data_utilities", "settings_backup_restore_sub",
            "backup_service_health_title", "backup_service_health_sub", "service_health_header",
            "service_health_active", "service_health_action_req", "service_health_role_granted",
            "service_health_role_not_granted", "backup_export_title", "backup_export_desc",
            "backup_export_password_label", "backup_export_btn", "backup_restore_title",
            "backup_restore_desc", "backup_import_btn", "backup_exported_dialog_title",
            "backup_exported_dialog_desc", "backup_copy_clipboard", "backup_import_dialog_title",
            "backup_import_dialog_desc", "backup_string_label", "backup_password_label",
            "backup_restore_btn", "backup_restored_success_toast", "backup_restore_failed_toast",
            "backup_copied_toast", "vcf_migration_title", "vcf_migration_desc",
            "btn_export_vcf", "btn_import_vcf", "vcf_exported_title", "vcf_exported_desc",
            "vcf_import_title", "vcf_import_desc", "vcf_content_label", "btn_import_contacts",
            "header_contacts_import_export", "settings_export_contacts_vcard",
            "settings_import_contacts_vcard", "settings_import_contacts_vcard_sub",
            "vcf_import_count_success", "btn_save_file", "btn_open_file",
            "btn_save_backup_file", "btn_restore_backup_file", "btn_save_vcf_file",
            "btn_choose_vcf_file", "file_saved_success", "file_save_failed", "file_read_failed"
        ]
    },
    {
        "title": "Settings - Security, Biometrics, PIN & App Lock",
        "keys": [
            "settings_security_protection", "settings_biometric_lock", "settings_biometric_lock_sub",
            "settings_pocket_protection", "settings_pocket_protection_sub", "pocket_lock_active",
            "pocket_lock_desc", "app_locked_title", "app_locked_desc", "btn_unlock_dialer",
            "device_auth_title", "device_auth_subtitle", "restricted_settings_title",
            "restricted_settings_desc", "btn_open_settings"
        ]
    },
    {
        "title": "Settings - Call Analytics, Dashboard & Call Notes",
        "keys": [
            "history_timeline", "call_details", "delete_all_history", "delete_call_log_entry",
            "no_matching_calls_for", "summary_missed_and_time", "summary_range_today",
            "summary_range_this_week", "summary_range_this_month", "summary_range_this_year",
            "summary_range_total", "total_talk_time", "summary_range_week_tab",
            "summary_range_month_tab", "summary_range_year_tab", "summary_range_all_tab",
            "settings_dashboard_style", "settings_dashboard_style_sub", "settings_call_log_dashboard",
            "settings_call_log_dashboard_sub", "settings_call_log_filters", "settings_call_log_filters_sub",
            "settings_all_call_notes", "settings_all_call_notes_sub", "no_notes_title",
            "no_notes_desc", "search_notes_placeholder", "edit_note_title", "delete_note_confirm",
            "note_deleted_toast", "call_note_card_title", "add_call_note_btn",
            "call_notes_count_title", "call_notes_count_badge", "jot_call_note_title",
            "call_note", "note_saved", "note_placeholder", "btn_save_note", "header_productivity_notes"
        ]
    },
    {
        "title": "Settings - Fake Call Simulator & Reminders",
        "keys": [
            "settings_fake_call_sim", "settings_fake_call_sim_sub", "fake_call_sim_title",
            "fake_call_sim_explanation", "caller_identity", "caller_name_label",
            "trigger_delay_timer", "duration_label", "sequential_multi_call_title",
            "sequential_multi_call_desc", "repeat_interval_label", "multi_call_note",
            "schedule_sequential_calls", "schedule_escape_call", "btn_cancel_scheduled_escape_calls",
            "unit_sec", "unit_min", "unit_hour", "unit_day", "callback_reminders_title",
            "callback_reminders_stats", "active_reminders_title", "no_active_reminders_title",
            "no_active_reminders_desc", "history_passed_title", "scheduled_prefix",
            "triggered_prefix", "schedule_callback_reminder_title", "schedule_callback_reminder_sub",
            "remind_call_back_prompt", "optional_note_label", "settings_callback_reminders",
            "settings_callback_reminders_sub", "btn_call_back"
        ]
    },
    {
        "title": "Settings - System Calling & Lock Screen Permissions",
        "keys": [
            "btn_grant", "btn_configure", "btn_set_default", "status_granted", "status_enabled",
            "settings_lockscreen_calling_title", "settings_lockscreen_calling_sub",
            "settings_full_screen_intent_warning", "settings_overlay_permission_warning",
            "settings_all_call_permissions_good", "state_enabled", "state_disabled",
            "feature_enabled_prompt", "feature_enable_prompt", "feature_disabled_hint", "btn_enable_feature",
            "permissions_required", "restricted_settings_btn"
        ]
    },
    {
        "title": "Settings - About, Privacy Policy & Community Support",
        "keys": [
            "settings_about", "settings_about_sub", "settings_privacy", "settings_privacy_sub",
            "settings_about_privacy", "about_mission_title", "about_mission_desc",
            "about_app_desc", "privacy_policy_title", "privacy_intro",
            "privacy_section_1_title", "privacy_section_1_desc", "privacy_section_2_title",
            "privacy_section_2_desc", "privacy_section_3_title", "privacy_section_3_desc",
            "settings_check_updates", "settings_check_updates_sub", "check_updates_desc",
            "checking_mirrors", "app_up_to_date_desc", "check_for_updates_now",
            "up_to_date_result", "settings_app_updates", "settings_app_updates_sub",
            "settings_updates_title", "settings_contribution", "settings_support_title",
            "settings_support_desc", "settings_contribute_github", "app_version_name"
        ]
    },
    {
        "title": "General Buttons, Common Utilities & Common Dialogs",
        "keys": [
            "btn_cancel", "btn_save", "btn_delete", "btn_edit", "btn_discard",
            "btn_import", "delete", "edit", "close", "add", "add_new_contact",
            "add_call", "add_call_search_hint", "filter_all_contacts", "account_filter_all",
            "not_set", "on_hold_prefix", "warning", "error_open_messages", "history"
        ]
    }
]

def load_strings(path):
    if not os.path.exists(path):
        return {}
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    # Clean bare ampersands
    text = re.sub(r'&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)', '&amp;', text)
    matches = re.findall(r'<string\s+name=\"([^\"]+)\"([^>]*)>(.*?)</string>', text, re.DOTALL)
    return {m[0]: (m[1].strip(), m[2].strip()) for m in matches}

def clean_xml_text(val):
    val = val.strip()
    val = re.sub(r'&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)', '&amp;', val)
    val = re.sub(r"(?<!\\)'", r"\'", val)
    return val

def run():
    en_path = os.path.join(RES_DIR, "values", "strings.xml")
    en_dict = load_strings(en_path)
    print(f"Loaded canonical EN strings: {len(en_dict)} keys")

    # Verify all mapped keys
    all_sec_keys = []
    for s in SECTIONS:
        for k in s["keys"]:
            if k in en_dict and k not in all_sec_keys:
                all_sec_keys.append(k)

    print(f"Total section keys matched with EN: {len(all_sec_keys)}")
    missing_from_secs = set(en_dict.keys()) - set(all_sec_keys)
    if missing_from_secs:
        print(f"Missing from sections: {missing_from_secs}")
        # Append missing to last section
        SECTIONS[-1]["keys"].extend(list(missing_from_secs))
        for k in missing_from_secs:
            all_sec_keys.append(k)
        print(f"After append, matched with EN: {len(all_sec_keys)}")

    # Load all locale dicts
    locale_dicts = {}
    for loc in LOCALES:
        p = os.path.join(RES_DIR, f"values-{loc}", "strings.xml")
        locale_dicts[loc] = load_strings(p)
        print(f"Loaded {loc}: {len(locale_dicts[loc])} keys")

    # Output writer
    def write_strings_file(out_path, loc_code=None):
        lines = []
        lines.append('<?xml version="1.0" encoding="utf-8"?>')
        lines.append('<!--')
        lines.append('  ~ Copyright (C) 2026 MovStore')
        lines.append('  ~')
        lines.append('  ~ This program is free software: you can redistribute it and/or modify')
        lines.append('  ~ it under the terms of the GNU General Public License as published by')
        lines.append('  ~ the Free Software Foundation, either version 3 of the License, or')
        lines.append('  ~ (at your option) any later version.')
        lines.append('  -->')
        lines.append('<resources>')

        written_keys = set()
        for sec in SECTIONS:
            lines.append(f'\n    <!-- {sec["title"]} -->')
            for k in sec["keys"]:
                if k not in en_dict or k in written_keys:
                    continue
                written_keys.add(k)
                en_attr, en_val = en_dict[k]
                
                # Attribute handling
                attr_str = f' {en_attr}' if en_attr else ''
                if k == "settings_support_desc":
                    attr_str = ' formatted="false"'

                if loc_code is None:
                    # English
                    val = clean_xml_text(en_val)
                else:
                    # Check translation override first
                    if loc_code in TRANSLATIONS and k in TRANSLATIONS[loc_code]:
                        val = clean_xml_text(TRANSLATIONS[loc_code][k])
                    elif k in locale_dicts[loc_code]:
                        loc_attr, loc_val = locale_dicts[loc_code][k]
                        val = clean_xml_text(loc_val)
                    else:
                        val = clean_xml_text(en_val)

                lines.append(f'    <string name="{k}"{attr_str}>{val}</string>')

        lines.append('</resources>\n')
        content = '\n'.join(lines)
        with open(out_path, 'w', encoding='utf-8') as f:
            f.write(content)

    # Write EN
    write_strings_file(en_path, None)
    print("Successfully wrote canonical EN strings.xml")

    # Write all locales
    for loc in LOCALES:
        p = os.path.join(RES_DIR, f"values-{loc}", "strings.xml")
        write_strings_file(p, loc)
        print(f"Successfully wrote {loc} strings.xml")

    # Validate XML parsing for all files
    for loc in [None] + LOCALES:
        p = en_path if loc is None else os.path.join(RES_DIR, f"values-{loc}", "strings.xml")
        try:
            tree = ET.parse(p)
            cnt = len(tree.getroot().findall("string"))
            tag = "EN" if loc is None else loc
            print(f"VALID XML [{tag}]: {cnt} strings")
            assert cnt == len(en_dict), f"Count mismatch in {tag}: {cnt} vs {len(en_dict)}"
        except Exception as e:
            print(f"ERROR parsing {p}: {e}")
            raise e

    print("ALL 10 FILES SYNCHRONIZED, STRUCTURED, AND VALIDATED!")

if __name__ == "__main__":
    run()
