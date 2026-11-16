// @vitest-environment jsdom
import {render,screen} from '@testing-library/react';
import {afterEach,describe,expect,it,vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api=vi.hoisted(()=>({get:vi.fn(),put:vi.fn()}));
vi.mock('../services/api',()=>({default:api}));
import Approvals from './Approvals';

describe('team approvals employee names',()=>{
  afterEach(()=>vi.resetAllMocks());

  it('renders the API applicant name in both team tables',async()=>{
    const request={id:11,applicantId:3,applicantName:'Mahesh Savkar',leaveType:'Annual Leave',startDate:'2026-11-18',endDate:'2026-11-25',workingDays:6,reason:'Personal',status:'APPROVED'};
    api.get.mockImplementation(url=>Promise.resolve({data:url==='/leaves/pending'?{content:[request],totalElements:1}:{content:[request],totalElements:1}}));

    render(<Approvals/>);

    expect((await screen.findAllByText('Mahesh Savkar')).length).toBe(2);
    expect(screen.queryByText('Employee #3')).not.toBeInTheDocument();
  });
});
