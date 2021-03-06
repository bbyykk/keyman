(*
  Name:             msctf
  Copyright:        Copyright (C) 2003-2017 SIL International.
  Documentation:
  Description:
  Create Date:      20 Nov 2012

  Modified Date:    28 May 2014
  Authors:          mcdurdin
  Related Files:
  Dependencies:

  Bugs:
  Todo:
  Notes:
  History:          20 Nov 2012 - mcdurdin - I3581 - V9.0 - KMTip needs to pass activated profile guid through to Keyman32 to switch keyboards
                    01 May 2014 - mcdurdin - I3743 - V9.0 - switch from ITfInputProcessorProfiles to ITfInputProcessorProfileMgr in kmcomapi lang install
                    28 May 2014 - mcdurdin - I4245 - V9.0 - msctf has reference to wrong type size for UINT_PTR
*)
unit keyman_msctf;

interface

uses
  System.Win.ComObj,
  System.Types,
  Winapi.ActiveX,
  Winapi.Windows;

const

  CLASS_TF_InputProcessorProfiles: TGUID = '{33C53A50-F456-4884-B049-85FD643ECFED}';
  CLASS_TF_CategoryMgr: TGUID = '{A4B544A1-438D-4B41-9325-869523E2D6C7}';

  LIBID_msctf: TGUID = '{98134007-285C-4B06-B5B1-F662A291BEFF}';

  IID_ITfThreadMgrEventSink: TGUID = '{AA80E80E-2021-11D2-93E0-0060B067B86E}';
  IID_ITfDocumentMgr: TGUID = '{AA80E7F4-2021-11D2-93E0-0060B067B86E}';
  IID_ITfContext: TGUID = '{AA80E7FD-2021-11D2-93E0-0060B067B86E}';
  IID_ITfEditSession: TGUID = '{AA80E803-2021-11D2-93E0-0060B067B86E}';
  IID_ITfRange: TGUID = '{AA80E7FF-2021-11D2-93E0-0060B067B86E}';
  IID_IDataObject: TGUID = '{0000010E-0000-0000-C000-000000000046}';
  IID_IEnumFORMATETC: TGUID = '{00000103-0000-0000-C000-000000000046}';
  IID_IAdviseSink: TGUID = '{0000010F-0000-0000-C000-000000000046}';
  IID_IPersist: TGUID = '{0000010C-0000-0000-C000-000000000046}';
  IID_IPersistStream: TGUID = '{00000109-0000-0000-C000-000000000046}';
  IID_IMoniker: TGUID = '{0000000F-0000-0000-C000-000000000046}';
  IID_ISequentialStream: TGUID = '{0C733A30-2A1C-11CE-ADE5-00AA0044773D}';
  IID_IStream: TGUID = '{0000000C-0000-0000-C000-000000000046}';
  IID_IBindCtx: TGUID = '{0000000E-0000-0000-C000-000000000046}';
  IID_IRunningObjectTable: TGUID = '{00000010-0000-0000-C000-000000000046}';
  IID_IEnumMoniker: TGUID = '{00000102-0000-0000-C000-000000000046}';
  IID_IEnumString: TGUID = '{00000101-0000-0000-C000-000000000046}';
  IID_IEnumSTATDATA: TGUID = '{00000105-0000-0000-C000-000000000046}';
  IID_ITfContextView: TGUID = '{2433BF8E-0F9B-435C-BA2C-180611978C30}';
  IID_IEnumTfContextViews: TGUID = '{F0C0F8DD-CF38-44E1-BB0F-68CF0D551C78}';
  IID_ITfReadOnlyProperty: TGUID = '{17D49A3D-F8B8-4B2F-B254-52319DD64C53}';
  IID_ITfProperty: TGUID = '{E2449660-9542-11D2-BF46-00105A2799B5}';
  IID_IEnumTfRanges: TGUID = '{F99D3F40-8E32-11D2-BF46-00105A2799B5}';
  IID_ITfPropertyStore: TGUID = '{6834B120-88CB-11D2-BF45-00105A2799B5}';
  IID_IEnumTfProperties: TGUID = '{19188CB0-ACA9-11D2-AFC5-00105A2799B5}';
  IID_ITfRangeBackup: TGUID = '{463A506D-6992-49D2-9B88-93D55E70BB16}';
  IID_IEnumTfContexts: TGUID = '{8F1A7EA6-1654-4502-A86E-B2902344D507}';
  IID_ITfTextInputProcessor: TGUID = '{AA80E7F7-2021-11D2-93E0-0060B067B86E}';
  IID_ITfThreadMgr: TGUID = '{AA80E801-2021-11D2-93E0-0060B067B86E}';
  IID_IEnumTfDocumentMgrs: TGUID = '{AA80E808-2021-11D2-93E0-0060B067B86E}';
  IID_ITfFunctionProvider: TGUID = '{101D6610-0990-11D3-8DF0-00105A2799B5}';
  IID_IEnumTfFunctionProviders: TGUID = '{E4B24DB0-0990-11D3-8DF0-00105A2799B5}';
  IID_ITfCompartmentMgr: TGUID = '{7DCF57AC-18AD-438B-824D-979BFFB74B7C}';
  IID_ITfCompartment: TGUID = '{BB08F7A9-607A-4384-8623-056892B64371}';
  IID_IEnumGUID: TGUID = '{0002E000-0000-0000-C000-000000000046}';
  IID_ITfRangeACP: TGUID = '{057A6296-029B-4154-B79A-0D461D4EA94C}';
  IID_ITfPersistentPropertyLoaderACP: TGUID = '{4EF89150-0807-11D3-8DF0-00105A2799B5}';
  IID_ITfKeyEventSink: TGUID = '{AA80E7F5-2021-11D2-93E0-0060B067B86E}';
  IID_ITfSource: TGUID = '{4EA48A35-60AE-446F-8FD6-E6A8D82459F7}';
  IID_ITfMouseSink: TGUID = '{A1ADAAA2-3A24-449D-AC96-5183E7F5C217}';
  IID_IEnumTfLanguageProfiles: TGUID = '{3D61BF11-AC5F-42C8-A4CB-931BCC28C744}';
  IID_ITfUIElement: TGUID = '{EA1EA137-19DF-11D7-A6D2-00065B84435C}';
  IID_IEnumTfUIElements: TGUID = '{887AA91E-ACBA-4931-84DA-3C5208CF543F}';
  IID_IEnumTfInputProcessorProfiles: TGUID = '{71C6E74D-0F28-11D8-A82A-00065B84435C}';
  IID_ITfThreadMgrEx: TGUID = '{3E90ADE3-7594-4CB0-BB58-69628F5F458C}';
  IID_ITfConfigureSystemKeystrokeFeed: TGUID = '{0D2C969A-BC9C-437C-84EE-951C49B1A764}';
  IID_ITfCompositionView: TGUID = '{D7540241-F9A1-4364-BEFC-DBCD2C4395B7}';
  IID_IEnumITfCompositionView: TGUID = '{5EFD22BA-7838-46CB-88E2-CADB14124F8F}';
  IID_ITfComposition: TGUID = '{20168D64-5A8F-4A5A-B7BD-CFA29F4D0FD9}';
  IID_ITfCompositionSink: TGUID = '{A781718C-579A-4B15-A280-32B8577ACC5E}';
  IID_ITfContextComposition: TGUID = '{D40C8AAE-AC92-4FC7-9A11-0EE0E23AA39B}';
  IID_ITfContextOwnerCompositionServices: TGUID = '{86462810-593B-4916-9764-19C08E9CE110}';
  IID_ITfContextOwnerCompositionSink: TGUID = '{5F20AA40-B57A-4F34-96AB-3576F377CC79}';
  IID_ITfQueryEmbedded: TGUID = '{0FAB9BDB-D250-4169-84E5-6BE118FDD7A8}';
  IID_ITfInsertAtSelection: TGUID = '{55CE16BA-3014-41C1-9CEB-FADE1446AC6C}';
  IID_ITfCleanupContextSink: TGUID = '{01689689-7ACB-4E9B-AB7C-7EA46B12B522}';
  IID_ITfCleanupContextDurationSink: TGUID = '{45C35144-154E-4797-BED8-D33AE7BF8794}';
  IID_IEnumTfPropertyValue: TGUID = '{8ED8981B-7C10-4D7D-9FB3-AB72E9C75F72}';
  IID_ITfMouseTracker: TGUID = '{09D146CD-A544-4132-925B-7AFA8EF322D0}';
  IID_ITfMouseTrackerACP: TGUID = '{3BDD78E2-C16E-47FD-B883-CE6FACC1A208}';
  IID_ITfEditRecord: TGUID = '{42D4D099-7C1A-4A89-B836-6C6F22160DF0}';
  IID_ITfTextEditSink: TGUID = '{8127D409-CCD3-4683-967A-B43D5B482BF7}';
  IID_ITfTextLayoutSink: TGUID = '{2AF2D06A-DD5B-4927-A0B4-54F19C91FADE}';
  IID_ITfStatusSink: TGUID = '{6B7D8D73-B267-4F69-B32E-1CA321CE4F45}';
  IID_ITfEditTransactionSink: TGUID = '{708FBF70-B520-416B-B06C-2C41AB44F8BA}';
  IID_ITfContextOwner: TGUID = '{AA80E80C-2021-11D2-93E0-0060B067B86E}';
  IID_ITfContextOwnerServices: TGUID = '{B23EB630-3E1C-11D3-A745-0050040AB407}';
  IID_ITfContextKeyEventSink: TGUID = '{0552BA5D-C835-4934-BF50-846AAA67432F}';
  IID_ITextStoreACPServices: TGUID = '{AA80E901-2021-11D2-93E0-0060B067B86E}';
  IID_ITfCreatePropertyStore: TGUID = '{2463FBF0-B0AF-11D2-AFC5-00105A2799B5}';
  IID_ITfCompartmentEventSink: TGUID = '{743ABD5F-F26D-48DF-8CC5-238492419B64}';
  IID_ITfFunction: TGUID = '{DB593490-098F-11D3-8DF0-00105A2799B5}';
  IID_ITfInputProcessorProfiles: TGUID = '{1F02B6C5-7842-4EE6-8A0B-9A24183A95CA}';
  IID_ITfInputProcessorProfilesEx: TGUID = '{892F230F-FE00-4A41-A98E-FCD6DE0D35EF}';
  IID_ITfInputProcessorProfileSubstituteLayout: TGUID = '{4FD67194-1002-4513-BFF2-C0DDF6258552}';
  IID_ITfActiveLanguageProfileNotifySink: TGUID = '{B246CB75-A93E-4652-BF8C-B3FE0CFD7E57}';
  IID_ITfLanguageProfileNotifySink: TGUID = '{43C9FE15-F494-4C17-9DE2-B8A4AC350AA8}';
  IID_ITfInputProcessorProfileMgr: TGUID = '{71C6E74C-0F28-11D8-A82A-00065B84435C}';
  IID_ITfInputProcessorProfileActivationSink: TGUID = '{71C6E74E-0F28-11D8-A82A-00065B84435C}';
  IID_ITfKeystrokeMgr: TGUID = '{AA80E7F0-2021-11D2-93E0-0060B067B86E}';
  IID_ITfKeyTraceEventSink: TGUID = '{1CD4C13B-1C36-4191-A70A-7F3E611F367D}';
  IID_ITfPreservedKeyNotifySink: TGUID = '{6F77C993-D2B1-446E-853E-5912EFC8A286}';
  IID_ITfMessagePump: TGUID = '{8F1B8AD8-0B6B-4874-90C5-BD76011E8F7C}';
  IID_ITfThreadFocusSink: TGUID = '{C0F1DB0C-3A20-405C-A303-96B6010A885F}';
  IID_ITfTextInputProcessorEx: TGUID = '{6E4E2102-F9CD-433D-B496-303CE03A6507}';
  IID_ITfClientId: TGUID = '{D60A7B49-1B9F-4BE2-B702-47E9DC05DEC3}';
  IID_ITfDisplayAttributeInfo: TGUID = '{70528852-2F26-4AEA-8C96-215150578932}';
  IID_IEnumTfDisplayAttributeInfo: TGUID = '{7CEF04D7-CB75-4E80-A7AB-5F5BC7D332DE}';
  IID_ITfDisplayAttributeProvider: TGUID = '{FEE47777-163C-4769-996A-6E9C50AD8F54}';
  IID_ITfDisplayAttributeMgr: TGUID = '{8DED7393-5DB1-475C-9E71-A39111B0FF67}';
  IID_ITfDisplayAttributeNotifySink: TGUID = '{AD56F402-E162-4F25-908F-7D577CF9BDA9}';
  IID_ITfCategoryMgr: TGUID = '{C3ACEFB5-F69D-4905-938F-FCADCF4BE830}';
  IID_ITfSourceSingle: TGUID = '{73131F9C-56A9-49DD-B0EE-D046633F7528}';
  IID_ITfUIElementMgr: TGUID = '{EA1EA135-19DF-11D7-A6D2-00065B84435C}';
  IID_ITfUIElementSink: TGUID = '{EA1EA136-19DF-11D7-A6D2-00065B84435C}';
  IID_ITfCandidateListUIElement: TGUID = '{EA1EA138-19DF-11D7-A6D2-00065B84435C}';
  IID_ITfCandidateListUIElementBehavior: TGUID = '{85FAD185-58CE-497A-9460-355366B64B9A}';
  IID_ITfReadingInformationUIElement: TGUID = '{EA1EA139-19DF-11D7-A6D2-00065B84435C}';
  IID_ITfTransitoryExtensionUIElement: TGUID = '{858F956A-972F-42A2-A2F2-0321E1ABE209}';
  IID_ITfTransitoryExtensionSink: TGUID = '{A615096F-1C57-4813-8A15-55EE6E5A839C}';
  IID_ITfToolTipUIElement: TGUID = '{52B18B5C-555D-46B2-B00A-FA680144FBDB}';
  IID_ITfReverseConversionList: TGUID = '{151D69F0-86F4-4674-B721-56911E797F47}';
  IID_ITfReverseConversion: TGUID = '{A415E162-157D-417D-8A8C-0AB26C7D2781}';
  IID_ITfReverseConversionMgr: TGUID = '{B643C236-C493-41B6-ABB3-692412775CC4}';

const
GUID_TFCAT_TIP_KEYBOARD: TGUID = '{34745C63-B2F0-4784-8B67-5E12C8701A31}';
GUID_TFCAT_TIP_SPEECH: TGUID = '{B5A73CD1-8355-426B-A161-259808F26B14}';
GUID_TFCAT_TIP_HANDWRITING: TGUID = '{246ECB87-C2F2-4ABE-905B-C8B38ADD2C43}';
GUID_TFCAT_TIPCAP_SECUREMODE: TGUID = '{49D2F9CE-1F5E-11D7-A6D3-00065B84435C}';
GUID_TFCAT_TIPCAP_UIELEMENTENABLED: TGUID = '{49D2F9CF-1F5E-11D7-A6D3-00065B84435C}';
GUID_TFCAT_TIPCAP_INPUTMODECOMPARTMENT: TGUID = '{CCF05DD7-4A87-11D7-A6E2-00065B84435C}';
GUID_TFCAT_TIPCAP_COMLESS: TGUID = '{364215D9-75BC-11D7-A6EF-00065B84435C}';
GUID_TFCAT_TIPCAP_WOW16: TGUID = '{364215DA-75BC-11D7-A6EF-00065B84435C}';
GUID_TFCAT_TIPCAP_IMMERSIVESUPPORT: TGUID = '{13A016DF-560B-46CD-947A-4C3AF1E0E35D}';
GUID_TFCAT_TIPCAP_SYSTRAYSUPPORT: TGUID = '{25504FB4-7BAB-4BC1-9C69-CF81890F0EF5}';

//
// This redefinition of TF_INPUTPROCESSORPROFILE and interfaces that use it, from
// Winapi.MsCTF, is here because the Winapi.MsCTF version has the wrong alignment
// for Win64, resulting in an 80-byte instead of an 88-byte structure. This has
// been reported at https://quality.embarcadero.com/browse/RSP-19669
//
// The corresponding issue in the Keyman issue tracker is #508:
//   https://github.com/keymanapp/keyman/issues/508
//
// Once this has been corrected by Embarcadero, we should remove this redefinition.
//

{$ALIGN 8}
type
  TF_INPUTPROCESSORPROFILE = record
    dwProfileType: LongWord;
    langid: Word;
    clsid: TGUID;
    guidProfile: TGUID;
    catid: TGUID;
    hklSubstitute: HKL;
    dwCaps: LongWord;
    HKL: HKL;
    dwFlags: LongWord;
  end;


// *********************************************************************//
// Interface: IEnumTfInputProcessorProfiles
// Flags:     (0)
// GUID:      {71C6E74D-0F28-11D8-A82A-00065B84435C}
// *********************************************************************//
  IEnumTfInputProcessorProfiles = interface(IUnknown)
    ['{71C6E74D-0F28-11D8-A82A-00065B84435C}']
    function Clone(out ppenum: IEnumTfInputProcessorProfiles): HResult; stdcall;
    function Next(ulCount: LongWord; out pProfile: TF_INPUTPROCESSORPROFILE; out pcFetch: LongWord): HResult; stdcall;
    function Reset: HResult; stdcall;
    function Skip(ulCount: LongWord): HResult; stdcall;
  end;
  {$EXTERNALSYM IEnumTfInputProcessorProfiles}

// *********************************************************************//
// Interface: ITfInputProcessorProfileMgr
// Flags:     (0)
// GUID:      {71C6E74C-0F28-11D8-A82A-00065B84435C}
// *********************************************************************//
  ITfInputProcessorProfileMgr = interface(IUnknown)
    ['{71C6E74C-0F28-11D8-A82A-00065B84435C}']
    function ActivateProfile(dwProfileType: LongWord; langid: Word; var clsid: TGUID;
                             var guidProfile: TGUID; HKL: HKL; dwFlags: LongWord): HResult; stdcall;
    function DeactivateProfile(dwProfileType: LongWord; langid: Word; var clsid: TGUID;
                               var guidProfile: TGUID; HKL: HKL; dwFlags: LongWord): HResult; stdcall;
    function GetProfile(dwProfileType: LongWord; langid: Word; var clsid: TGUID;
                        var guidProfile: TGUID; HKL: HKL; out pProfile: TF_INPUTPROCESSORPROFILE): HResult; stdcall;
    function EnumProfiles(langid: Word; out ppenum: IEnumTfInputProcessorProfiles): HResult; stdcall;
    function ReleaseInputProcessor(var rclsid: TGUID; dwFlags: LongWord): HResult; stdcall;
// !! "var word" -> PWideChar
    function RegisterProfile(var rclsid: TGUID; langid: Word; var guidProfile: TGUID;
                             pchDesc: PWideChar; cchDesc: LongWord; pchIconFile: PWideChar;
                             cchFile: LongWord; uIconIndex: LongWord; hklSubstitute: HKL;
                             dwPreferredLayout: LongWord; bEnabledByDefault: Integer;
                             dwFlags: LongWord): HResult; stdcall;
    function UnregisterProfile(var rclsid: TGUID; langid: Word; var guidProfile: TGUID;
                               dwFlags: LongWord): HResult; stdcall;
    function GetActiveProfile(var catid: TGUID; out pProfile: TF_INPUTPROCESSORPROFILE): HResult; stdcall;
  end;
  {$EXTERNALSYM ITfInputProcessorProfileMgr}

implementation

end.
