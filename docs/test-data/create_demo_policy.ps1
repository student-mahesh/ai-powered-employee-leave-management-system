$ErrorActionPreference = 'Stop'
$out = Join-Path $PSScriptRoot 'demo-hr-leave-policy.pdf'
$lines = @(
  'DEMO HR LEAVE POLICY - FOR SOFTWARE TESTING ONLY',
  'This fictional document is test data for the AI Employee Leave Management System.',
  'It is not an actual company policy and creates no employment rights.',
  '',
  '1. Leave types',
  'The demo system currently configures Annual Leave and Sick Leave as active leave types.',
  'Leave types are configured by an administrator and may vary by deployment.',
  '',
  '2. Eligibility and balances',
  'The test employee has a demo entitlement of 20 Annual Leave days.',
  'The application reads the authenticated employee balance from the system database.',
  'The application rejects balance debits when the remaining balance is insufficient.',
  '',
  '3. Applying for leave',
  'Employees submit a leave type, start date, end date, and reason.',
  'Dates cannot be in the past, the end date cannot precede the start date,',
  'overlapping requests are rejected, and a request must include a working day.',
  'Working days exclude Saturdays, Sundays, and configured company holidays.',
  '',
  '4. Approval and cancellation',
  'A submitted request starts with PENDING status.',
  'A manager may review a direct report request; an administrator may review requests.',
  'The AI assistant cannot approve or reject leave requests.',
  'Employees may cancel their own pending request. Future approved leave may be cancelled',
  'and its balance is restored by the normal application service.',
  '',
  '5. AI assistant safety',
  'Policy answers must use uploaded policy evidence and include source page citations.',
  'If the uploaded policies do not contain evidence, the assistant must say so.',
  'Leave submission requires explicit confirmation of the exact request details.',
  'The assistant cannot execute arbitrary SQL or directly modify balances.'
)
function Escape-PdfText([string]$value) { $value.Replace('\', '\\').Replace('(', '\(').Replace(')', '\)') }
$streamLines = @('BT', '/F1 10 Tf', '50 760 Td')
foreach ($line in $lines) {
  if ($line -eq '') { $streamLines += '0 -16 Td' } else { $streamLines += "($(Escape-PdfText $line)) Tj"; $streamLines += '0 -16 Td' }
}
$streamLines += 'ET'
$stream = ($streamLines -join "`n") + "`n"
$objects = @(
  '<< /Type /Catalog /Pages 2 0 R >>',
  '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
  '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
  '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  "<< /Length $([Text.Encoding]::ASCII.GetByteCount($stream)) >>`nstream`n$stream`nendstream"
)
$pdf = "%PDF-1.4`n"
$offsets = @(0)
for ($i = 0; $i -lt $objects.Count; $i++) { $offsets += [Text.Encoding]::ASCII.GetByteCount($pdf); $pdf += "$($i + 1) 0 obj`n$($objects[$i])`nendobj`n" }
$xref = [Text.Encoding]::ASCII.GetByteCount($pdf)
$pdf += "xref`n0 $($objects.Count + 1)`n0000000000 65535 f `n"
for ($i = 1; $i -lt $offsets.Count; $i++) { $pdf += ('{0:0000000000} 00000 n ' -f $offsets[$i]) + "`n" }
$pdf += "trailer`n<< /Size $($objects.Count + 1) /Root 1 0 R >>`nstartxref`n$xref`n%%EOF`n"
[IO.File]::WriteAllBytes($out, [Text.Encoding]::ASCII.GetBytes($pdf))
Write-Output $out
